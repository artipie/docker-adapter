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

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Manifest PUT endpoint.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ManifestEntityPutTest {

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    /**
     * Docker used in tests.
     */
    private Docker docker;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
        this.slice = new DockerSlice("/base", this.docker);
    }

    @Test
    void shouldPushManifestByTag() {
        final String path = "/v2/my-alpine/manifests/1";
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("PUT", String.format("/base%s", path), "HTTP/1.1").toString(),
                Collections.emptyList(),
                this.manifest()
            ),
            Matchers.allOf(
                new RsHasStatus(RsStatus.CREATED),
                new RsHasHeaders(
                    new Header("Location", path),
                    new Header("Content-Length", "0"),
                    new Header(
                        "Docker-Content-Digest",
                        "sha256:02b9f91901050f814adfb19b1a8f5d599b07504998c2d665baa82e364322b566"
                    )
                )
            )
        );
    }

    @Test
    void shouldPushManifestByDigest() {
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "02b9f91901050f814adfb19b1a8f5d599b07504998c2d665baa82e364322b566"
        );
        final String path = String.format("/v2/my-alpine/manifests/%s", digest);
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("PUT", String.format("/base%s", path), "HTTP/1.1").toString(),
                Collections.emptyList(),
                this.manifest()
            ),
            Matchers.allOf(
                new RsHasStatus(RsStatus.CREATED),
                new RsHasHeaders(
                    new Header("Location", path),
                    new Header("Content-Length", "0"),
                    new Header("Docker-Content-Digest", digest)
                )
            )
        );
    }

    /**
     * Create manifest content.
     *
     * @return Manifest content.
     */
    private Flowable<ByteBuffer> manifest() {
        final byte[] content = "config".getBytes();
        final Blob config = this.docker.repo(new RepoName.Valid("my-alpine")).layers()
            .put(new Content.From(content), new Digest.Sha256(content))
            .toCompletableFuture().join();
        final byte[] data = String.format(
            "{\"config\":{\"digest\":\"%s\"},\"layers\":[]}",
            config.digest().string()
        ).getBytes();
        return Flowable.just(ByteBuffer.wrap(data));
    }
}
