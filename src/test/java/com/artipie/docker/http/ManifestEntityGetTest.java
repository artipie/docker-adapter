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

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.docker.ExampleStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Manifest GET endpoint.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ManifestEntityGetTest {

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.slice = new DockerSlice("/base", new AstoDocker(new ExampleStorage()));
    }

    @Test
    void shouldReturnManifestByTag() throws Exception {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("GET", "/base/v2/my-alpine/manifests/1", "HTTP/1.1").toString(),
                Collections.singleton(
                    new Header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
                ),
                Flowable.empty()
            ),
            success(
                "sha256:cb8a924afdf0229ef7515d9e5b3024e23b3eb03ddbba287f4a19c6ac90b8d221",
                new Key.From(
                    "docker", "registry", "v2", "blobs", "sha256", "cb",
                    "cb8a924afdf0229ef7515d9e5b3024e23b3eb03ddbba287f4a19c6ac90b8d221", "data"
                )
            )
        );
    }

    @Test
    void shouldReturnManifestByDigest() throws Exception {
        final String hex = "cb8a924afdf0229ef7515d9e5b3024e23b3eb03ddbba287f4a19c6ac90b8d221";
        final String digest = String.format("%s:%s", "sha256", hex);
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(
                    "GET",
                    String.format("/base/v2/my-alpine/manifests/%s", digest),
                    "HTTP/1.1"
                ).toString(),
                Collections.singleton(
                    new Header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
                ),
                Flowable.empty()
            ),
            success(
                digest,
                new Key.From("docker", "registry", "v2", "blobs", "sha256", "cb", hex, "data")
            )
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownTag() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("GET", "/base/v2/my-alpine/manifests/2", "HTTP/1.1").toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownDigest() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(
                    "GET",
                    String.format(
                        "/base/v2/my-alpine/manifests/%s",
                        "sha256:0123456789012345678901234567890123456789012345678901234567890123"
                    ),
                    "HTTP/1.1"
                ).toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    private static Matcher<Response> success(
        final String digest,
        final Key content
    ) throws Exception {
        return new AllOf<>(
            Arrays.asList(
                new RsHasStatus(RsStatus.OK),
                new RsHasHeaders(
                    new Header(
                        "Content-Type",
                        "application/vnd.docker.distribution.manifest.v2+json"
                    ),
                    new Header("Docker-Content-Digest", digest)
                ),
                new RsHasBody(
                    new BlockingStorage(new ExampleStorage()).value(content)
                )
            )
        );
    }
}
