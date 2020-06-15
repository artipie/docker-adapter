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

import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Blob entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#blob">Blob</a>.
 *
 * @since 0.2
 */
final class BlobEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/v2/(?<name>[^/]*)/blobs/(?<digest>(?!(uploads/)).*)$"
    );

    /**
     * Ctor.
     */
    private BlobEntity() {
    }

    /**
     * Slice for GET method.
     *
     * @since 0.2
     */
    static final class Get implements Slice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Get(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final Digest digest = request.digest();
            return new AsyncResponse(
                this.docker.repo(request.name()).layers().get(digest).thenApply(
                    found -> found.<Response>map(
                        blob -> new AsyncResponse(
                            blob.content().thenCompose(
                                content -> content.size()
                                    .map(CompletableFuture::completedStage)
                                    .orElseGet(blob::size)
                                    .thenApply(
                                        size -> new RsWithBody(
                                            new BaseResponse(digest, size),
                                            content
                                        )
                                    )
                            )
                        )
                    ).orElseGet(
                        () -> new RsWithStatus(RsStatus.NOT_FOUND)
                    )
                )
            );
        }
    }

    /**
     * Slice for HEAD method.
     *
     * @since 0.2
     */
    static final class Head implements Slice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Head(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            return new AsyncResponse(
                this.docker.repo(request.name()).layers().get(request.digest()).thenApply(
                    found -> found.<Response>map(
                        blob -> new AsyncResponse(
                            blob.size().thenApply(size -> new BaseResponse(blob.digest(), size))
                        )
                    ).orElseGet(
                        () -> new RsWithStatus(RsStatus.NOT_FOUND)
                    )
                )
            );
        }
    }

    /**
     * Blob base response.
     *
     * @since 0.2
     */
    private static class BaseResponse extends Response.Wrap {

        /**
         * Ctor.
         *
         * @param digest Blob digest.
         * @param size Blob size.
         */
        BaseResponse(final Digest digest, final long size) {
            super(
                new RsWithHeaders(
                    new RsWithStatus(RsStatus.OK),
                    new ContentLength(String.valueOf(size)),
                    new DigestHeader(digest),
                    new ContentType("application/octet-stream")
                )
            );
        }
    }

    /**
     * HTTP request to blob entity.
     *
     * @since 0.2
     * @todo #167:30min Add test coverage for `BlobEntity.Request` class.
     *  This class lacks test coverage. It should be tested both against
     *  valid and invalid request lines.
     */
    private static final class Request {

        /**
         * HTTP request line.
         */
        private final String line;

        /**
         * Ctor.
         *
         * @param line HTTP request line.
         */
        Request(final String line) {
            this.line = line;
        }

        /**
         * Get repository name.
         *
         * @return Repository name.
         */
        RepoName name() {
            return new RepoName.Valid(this.path().group("name"));
        }

        /**
         * Get digest.
         *
         * @return Digest.
         */
        Digest digest() {
            return new Digest.FromString(this.path().group("digest"));
        }

        /**
         * Matches request path by RegEx pattern.
         *
         * @return Path matcher.
         */
        private Matcher path() {
            final String path = new RequestLineFrom(this.line).uri().getPath();
            final Matcher matcher = PATH.matcher(path);
            if (!matcher.matches()) {
                throw new IllegalStateException(String.format("Unexpected path: %s", path));
            }
            return matcher;
        }
    }
}
