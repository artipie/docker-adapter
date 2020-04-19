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

import com.artipie.docker.ExampleStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.Assertion;
import org.llorllale.cactoos.matchers.Throws;

/**
 * Tests for {@link DockerSlice}.
 * Start upload endpoint.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class StartUploadTest {

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.slice = new DockerSlice(new AstoDocker(new ExampleStorage()));
    }

    @Test
    void shouldReturnInitialUploadStatus() {
        final Response response = this.slice.response(
            new RequestLine(
                "POST",
                "/v2/test/blobs/uploads/",
                "HTTP/1.1"
            ).toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.ACCEPTED),
                    new RsHasHeaders(
                        Matchers.contains(
                            new IsHeader(
                                "Location",
                                new StringStartsWith(false, "/v2/test/blobs/uploads/")
                            ),
                            new IsHeader("Range", "bytes=0-0"),
                            new IsHeader("Content-Length", "0"),
                            new IsHeader("Docker-Upload-UUID", new IsNot<>(Matchers.emptyString()))
                        )
                    )
                )
            )
        );
    }

    /*
     * @todo #81:30min StartUpload is not behaving correctly
     *  StartUpload should throw an exception in case of an unexpected path,
     *  but it isn't working. Correct the code and enable this test.
     */
    @Test
    @Disabled
    void shouldThrowExceptionOnUnexpectedPath() {
        new Assertion<>(
            "Did not throw exception",
            () -> this.slice.response(
                new RequestLine(
                    "POST",
                    "/v2/unexpected/path/exception",
                    "HTTP/1.1"
                ).toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new Throws<>(
                "Unexpected path: /v2/unexpected/path/exception",
                IllegalStateException.class
            )
        ).affirm();
    }
}
