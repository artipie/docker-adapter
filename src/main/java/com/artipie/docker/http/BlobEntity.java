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
        "^/v2/[^/]*/blobs/(?<digest>(?!(uploads/)).*)$"
    );

    /**
     * Ctor.
     */
    private BlobEntity() {
    }

    /**
     * Read digest from HTTP request line.
     *
     * @param line HTTP request line.
     * @return Digest.
     */
    private static Digest digest(final String line) {
        final String path = new RequestLineFrom(line).uri().getPath();
        final Matcher matcher = PATH.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalStateException(String.format("Unexpected path: %s", path));
        }
        return new Digest.FromString(matcher.group("digest"));
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
            final Digest digest = digest(line);
            return new AsyncResponse(
                this.docker.blobStore().blob(digest).thenApply(
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
            final Digest digest = digest(line);
            return new AsyncResponse(
                this.docker.blobStore().blob(digest).thenApply(
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
}
