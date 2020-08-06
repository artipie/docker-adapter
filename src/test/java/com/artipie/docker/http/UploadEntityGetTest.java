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
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.Response;
import com.artipie.http.auth.Permissions;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Upload GET endpoint.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidDuplicateLiterals"})
public final class UploadEntityGetTest {
    /**
     * Docker registry used in tests.
     */
    private Docker docker;

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
        this.slice = new DockerSlice(
            this.docker,
            new Permissions.Single(TestAuthentication.USERNAME, DockerSlice.READ),
            new TestAuthentication()
        );
    }

    @Test
    void shouldReturnZeroOffsetAfterUploadStarted() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name))
            .uploads()
            .start()
            .toCompletableFuture().join();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, upload.uuid());
        final Response response = this.slice.response(
            new RequestLine(RqMethod.GET, String.format("%s", path)).toString(),
            new TestAuthentication.Headers(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new ResponseMatcher(
                RsStatus.NO_CONTENT,
                new Header("Range", "0-0"),
                new Header("Content-Length", "0"),
                new Header("Docker-Upload-UUID", upload.uuid())
            )
        );
    }

    @Test
    void shouldReturnZeroOffsetAfterOneByteUploaded() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name))
            .uploads()
            .start()
            .toCompletableFuture().join();
        upload.append(new Content.From(new byte[1])).toCompletableFuture().join();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, upload.uuid());
        final Response response = this.slice.response(
            new RequestLine(RqMethod.GET, String.format("%s", path)).toString(),
            new TestAuthentication.Headers(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new ResponseMatcher(
                RsStatus.NO_CONTENT,
                new Header("Range", "0-0"),
                new Header("Content-Length", "0"),
                new Header("Docker-Upload-UUID", upload.uuid())
            )
        );
    }

    @Test
    void shouldReturnOffsetDuringUpload() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name))
            .uploads()
            .start()
            .toCompletableFuture().join();
        // @checkstyle MagicNumberCheck (1 line)
        upload.append(new Content.From(new byte[128])).toCompletableFuture().join();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, upload.uuid());
        final Response get = this.slice.response(
            new RequestLine(RqMethod.GET, String.format("%s", path)).toString(),
            new TestAuthentication.Headers(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            get,
            new ResponseMatcher(
                RsStatus.NO_CONTENT,
                new Header("Range", "0-127"),
                new Header("Content-Length", "0"),
                new Header("Docker-Upload-UUID", upload.uuid())
            )
        );
    }

    @Test
    void shouldReturnNotFoundWhenUploadNotExists() {
        final Response response = this.slice.response(
            new RequestLine(RqMethod.GET, "/v2/test/blobs/uploads/12345").toString(),
            new TestAuthentication.Headers(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new IsErrorsResponse(RsStatus.NOT_FOUND, "BLOB_UPLOAD_UNKNOWN")
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenNoAuth() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.GET, "/v2/test/blobs/uploads/123").toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.UNAUTHORIZED)
        );
    }
}
