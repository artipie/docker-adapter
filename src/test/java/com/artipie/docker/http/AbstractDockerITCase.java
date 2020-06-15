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

import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.google.common.collect.ImmutableList;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Abstract class for integration tests using docker command.
 *
 * @since 0.3
 * @todo #187:60min Implement JUnit extension instead of abstract class.
 *  After that, remove {@link AbstractDockerITCase} and use extension in
 *  {@link DockerSliceITCase} and {@link LargeImageITCase}.
 */
public class AbstractDockerITCase {
    /**
     * Vert.x instance to use in tests.
     */
    private static Vertx instance;

    /**
     * Working directory for docker command.
     */
    private Path dir;

    /**
     * HTTP server hosting repository.
     */
    private VertxSliceServer server;

    /**
     * Setup environment for integration tests (set working dir for
     * <code>docker</code> command and start vertx server).
     *
     * @param wrkdir Working directory for docker
     * @param slice Slice to run
     * @return Docker repository base URL
     * @throws Exception
     */
    protected final String startServer(final Path wrkdir, final DockerSlice slice)
        throws Exception {
        this.dir = wrkdir;
        this.ensureDockerInstalled();
        this.server = new VertxSliceServer(
            AbstractDockerITCase.instance,
            new LoggingSlice(slice)
        );
        final int port = this.server.start();
        return String.format("localhost:%s", port);
    }

    protected final void stopServer() {
        if (this.server != null) {
            this.server.stop();
        }
    }

    protected final Vertx vertx() {
        return AbstractDockerITCase.instance;
    }

    protected final String run(final String... args) throws Exception {
        final Path stdout = this.dir.resolve(
            String.format("%s-stdout.txt", UUID.randomUUID().toString())
        );
        final List<String> command = ImmutableList.<String>builder()
            .add("docker")
            .add(args)
            .build();
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        final int code = new ProcessBuilder()
            .directory(this.dir.toFile())
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

    @BeforeAll
    static void init() {
        AbstractDockerITCase.instance = Vertx.vertx();
    }

    @AfterAll
    static void destroy() {
        AbstractDockerITCase.instance.close();
    }

    private void ensureDockerInstalled() throws Exception {
        final String output = this.run("--version");
        if (!output.startsWith("Docker version")) {
            throw new IllegalStateException("Docker not installed");
        }
    }
}
