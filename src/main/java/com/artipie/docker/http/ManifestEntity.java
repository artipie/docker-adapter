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
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Manifest entity in Docker HTTP API..
 * See <a href="https://docs.docker.com/registry/spec/api/#manifest">Manifest</a>.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ManifestEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/v2/(?<name>[^/]*)/manifests/(?<reference>.*)$"
    );

    /**
     * Ctor.
     */
    private ManifestEntity() {
    }

    /**
     * Slice for HEAD method, checking manifest existence.
     *
     * @since 0.2
     */
    public static class Head implements Slice {

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
            final Publisher<ByteBuffer> body) {
            final Request request = new Request(line);
            return new AsyncResponse(
                this.docker.repo(request.name()).manifests().get(request.reference()).thenCompose(
                    manifest -> manifest.<CompletionStage<Response>>map(
                        found -> found.convert(Head.acceptHeader(headers))
                            .thenCompose(
                                original -> CompletableFuture.completedStage(
                                    new BaseResponse(original)
                                )
                            )
                    ).orElseGet(
                        () -> CompletableFuture.completedStage(new RsWithStatus(RsStatus.NOT_FOUND))
                    )
                )
            );
        }

        /**
         * Gets accept header.
         * @param headers Request headers
         * @return Accept headers
         */
        private static RqHeaders acceptHeader(final Iterable<Map.Entry<String, String>> headers) {
            return new RqHeaders(headers, "Accept");
        }
    }

    /**
     * Slice for GET method, getting manifest content.
     *
     * @since 0.2
     */
    public static class Get implements Slice {

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
            final RepoName name = request.name();
            final ManifestRef ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().get(ref).thenCompose(
                    manifest -> manifest.<CompletionStage<Response>>map(
                        found -> found.convert(Head.acceptHeader(headers))
                            .thenCompose(
                                original -> CompletableFuture.completedStage(
                                    new RsWithBody(new BaseResponse(original), original.content())
                                )
                            )
                    ).orElseGet(
                        () -> CompletableFuture.completedStage(new RsWithStatus(RsStatus.NOT_FOUND))
                    )
                )
            );
        }

    }

    /**
     * Slice for PUT method, uploading manifest content.
     *
     * @since 0.2
     */
    public static class Put implements Slice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Put(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final RepoName name = request.name();
            final ManifestRef ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().put(ref, new Content.From(body)).thenApply(
                    manifest -> new RsWithHeaders(
                        new RsWithStatus(RsStatus.CREATED),
                        new Header(
                            "Location",
                            String.format("/v2/%s/manifests/%s", name.value(), ref.string())
                        ),
                        new ContentLength("0"),
                        new DigestHeader(manifest.digest())
                    )
                )
            );
        }
    }

    /**
     * HTTP request to manifest entity.
     *
     * @since 0.2
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
         * Get manifest reference.
         *
         * @return Manifest reference.
         */
        ManifestRef reference() {
            return new ManifestRef.FromString(this.path().group("reference"));
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

    /**
     * Manifest base response.
     * @since 0.2
     */
    static final class BaseResponse extends Response.Wrap {

        /**
         * Ctor.
         *
         * @param mnf Manifest
         */
        BaseResponse(final Manifest mnf) {
            super(
                new AsyncResponse(
                    mnf.mediaType().thenApply(
                        type -> new RsWithHeaders(
                            StandardRs.EMPTY,
                            new ContentType(type),
                            new DigestHeader(mnf.digest())
                        )
                    )
                )
            );
        }

    }
}
