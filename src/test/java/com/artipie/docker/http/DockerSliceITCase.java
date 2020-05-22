/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.docker.http;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.google.common.collect.ImmutableList;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link DockerSlice}.
 *
 * @todo #54:30min Support tests running on Windows platform
 *  Docker client on Windows cannot pull Linux images.
 *  This requires to select image for tests depending on `os.name` property
 *  and redesign pull tests to push images first.
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class DockerSliceITCase {

    // @checkstyle VisibilityModifierCheck (5 lines)
    /**
     * Temporary directory.
     */
    @TempDir
    Path temp;

    /**
     * Vert.x instance to use in tests.
     */
    private Vertx vertx;

    /**
     * HTTP server hosting repository.
     */
    private VertxSliceServer server;

    /**
     * Repository URL.
     */
    private String repo;

    /**
     * Example image to use in tests.
     */
    private Image image;

    @BeforeEach
    void setUp() throws Exception {
        this.vertx = Vertx.vertx();
        this.ensureDockerInstalled();
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(new DockerSlice(new AstoDocker(new InMemoryStorage())))
        );
        this.image = this.prepareImage();
        final int port = this.server.start();
        this.repo = String.format("localhost:%s", port);
    }

    @AfterEach
    void tearDown() {
        if (this.server != null) {
            this.server.stop();
        }
        if (this.vertx != null) {
            this.vertx.close();
        }
    }

    @Test
    void shouldPush() throws Exception {
        final String output = this.run("push", this.image.remote());
        MatcherAssert.assertThat(
            output,
            Matchers.allOf(this.image.layersPushed(), this.image.manifestPushed())
        );
    }

    @Test
    void shouldPushExisting() throws Exception {
        this.run("push", this.image.remote());
        final String output = this.run("push", this.image.remote());
        MatcherAssert.assertThat(
            output,
            Matchers.allOf(this.image.layersAlreadyExist(), this.image.manifestPushed())
        );
    }

    @Test
    void shouldPullPushedByTag() throws Exception {
        this.run("push", this.image.remote());
        this.run("image", "rm", this.image.local());
        this.run("image", "rm", this.image.remote());
        final String output = this.run("pull", this.image.remote());
        MatcherAssert.assertThat(
            output,
            new StringContains(
                false,
                String.format("Status: Downloaded newer image for %s", this.image.remote())
            )
        );
    }

    @Test
    void shouldPullPushedByDigest() throws Exception {
        this.run("push", this.image.remote());
        this.run("image", "rm", this.image.local());
        this.run("image", "rm", this.image.remote());
        final String output = this.run("pull", this.image.remoteByDigest());
        MatcherAssert.assertThat(
            output,
            new StringContains(
                false,
                String.format("Status: Downloaded newer image for %s", this.image.remoteByDigest())
            )
        );
    }

    private String run(final String... args) throws Exception {
        final Path stdout = this.temp.resolve(
            String.format("%s-stdout.txt", UUID.randomUUID().toString())
        );
        final List<String> command = ImmutableList.<String>builder()
            .add("docker")
            .add(args)
            .build();
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        final int code = new ProcessBuilder()
            .directory(this.temp.toFile())
            .command(command)
            .redirectOutput(stdout.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(stdout));
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        if (code != 0) {
            throw new IllegalStateException(String.format("Not OK exit code: %d", code));
        }
        return log;
    }

    private void ensureDockerInstalled() throws Exception {
        final String output = this.run("--version");
        if (!output.startsWith("Docker version")) {
            throw new IllegalStateException("Docker not installed");
        }
    }

    private Image prepareImage() throws Exception {
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "a7766145a775d39e53a713c75b6fd6d318740e70327aaa3ed5d09e0ef33fc3df"
        );
        final String original = String.format("busybox@%s", digest);
        this.run("pull", original);
        final String local = "my-test";
        this.run("tag", original, String.format("%s:latest", local));
        final Image img = new Image(local, digest, "1079c30efc82");
        this.run("tag", original, img.remote());
        return img;
    }

    /**
     * Docker image info.
     *
     * @since 0.2
     */
    private class Image {

        /**
         * Image name.
         */
        private final String name;

        /**
         * Manifest digest.
         */
        private final String digest;

        /**
         * Short layer hash.
         */
        private final String layer;

        Image(final String name, final String digest, final String layer) {
            this.name = name;
            this.digest = digest;
            this.layer = layer;
        }

        public String local() {
            return this.name;
        }

        public String remote() {
            return String.format("%s/%s", DockerSliceITCase.this.repo, this.local());
        }

        public String remoteByDigest() {
            return String.format("%s@%s", this.remote(), this.digest);
        }

        public Matcher<String> manifestPushed() {
            return new StringContains(false, String.format("latest: digest: %s", this.digest));
        }

        public Matcher<String> layersPushed() {
            return new StringContains(false, String.format("%s: Pushed", this.layer));
        }

        public Matcher<String> layersAlreadyExist() {
            return new StringContains(false, String.format("%s: Layer already exists", this.layer));
        }
    }
}
