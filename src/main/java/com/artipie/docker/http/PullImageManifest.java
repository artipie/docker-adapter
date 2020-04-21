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

import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice for pull image manifest endpoint.
 * See <a href="https://docs.docker.com/registry/spec/api/#pulling-an-image">Pulling An Image</a>.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class PullImageManifest {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/v2/(?<name>[^/]*)/manifests/(?<reference>.*)$"
    );

    /**
     * Ctor.
     */
    private PullImageManifest() {
    }

    /**
     * Slice for HEAD method, checking manifest existence.
     *
     * @since 0.2
     */
    public static class Head implements Slice {

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
            return new RsWithStatus(RsStatus.OK);
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
            final String path = new RequestLineFrom(line).uri().getPath();
            final Matcher matcher = PATH.matcher(path);
            if (!matcher.find()) {
                throw new IllegalStateException(
                    String.format("Unexpected path: %s", path)
                );
            }
            final RepoName name = new RepoName.Valid(matcher.group("name"));
            final ManifestRef ref = new ManifestRef.FromString(matcher.group("reference"));
            return new AsyncResponse(
                this.docker.repo(name).manifest(ref).thenCompose(
                    manifest -> manifest.map(
                        original -> original.convert(new RqHeaders(headers, "Accept"))
                            .thenCompose(Get::response)
                    ).orElseGet(
                        () -> CompletableFuture.completedStage(new RsWithStatus(RsStatus.NOT_FOUND))
                    )
                )
            );
        }

        /**
         * Create HTTP response from {@link Manifest}.
         *
         * @param manifest Manifest.
         * @return HTTP response.
         */
        private static CompletionStage<Response> response(final Manifest manifest) {
            return manifest.mediaType().thenApply(
                type -> new RsWithBody(
                    new RsWithHeaders(new RsWithStatus(RsStatus.OK), "Content-Type", type),
                    manifest.content()
                )
            );
        }
    }
}
