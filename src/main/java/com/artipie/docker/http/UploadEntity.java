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
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.DigestFromContent;
import com.artipie.http.Connection;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Blob Upload entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#initiate-blob-upload">Initiate Blob Upload</a>
 * and <a href="https://docs.docker.com/registry/spec/api/#blob-upload">Blob Upload</a>.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class UploadEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/v2/(?<name>.*)/blobs/uploads/(?<uuid>.*)$"
    );

    /**
     * Ctor.
     */
    private UploadEntity() {
    }

    /**
     * Slice for POST method.
     *
     * @since 0.2
     */
    public static final class Post implements Slice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Post(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final RepoName name = new Request(line).name();
            return new AsyncResponse(
                this.docker.repo(name).uploads().start().thenApply(
                    upload -> new StatusResponse(name, upload.uuid(), 0)
                )
            );
        }
    }

    /**
     * Slice for PATCH method.
     *
     * @since 0.2
     */
    public static final class Patch implements Slice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Patch(final Docker docker) {
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
            final String uuid = request.uuid();
            return new AsyncResponse(
                this.docker.repo(name).uploads().get(uuid).thenCompose(
                    found -> found.<CompletionStage<Response>>map(
                        upload -> upload.append(body).thenApply(
                            offset -> new StatusResponse(name, uuid, offset)
                        )
                    ).orElseGet(
                        () -> CompletableFuture.completedStage(new RsWithStatus(RsStatus.NOT_FOUND))
                    )
                )
            );
        }
    }

    /**
     * Slice for PUT method.
     *
     * @since 0.2
     * @todo #137:30min Figure out whether or not should uploaded data be removed if digests do not
     *  match. There is no direct answer in docs, so this should be check experimentally with real
     *  docker registry.
     * @todo #142:30min Content is read twice while blob is added: now we first read it to compare
     *  digests and then to write upload as a blob. Such approach is inefficient and should be
     *  fixed. One of the possible solution is moving the comparison logic inside
     *  BlobStore.put() method.
     */
    public static final class Put implements Slice {

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
            final String uuid = request.uuid();
            final Repo repo = this.docker.repo(name);
            return new AsyncResponse(
                repo.uploads().get(uuid).<Response>thenCompose(
                    found -> found.map(
                        upload -> upload.content()
                            .thenCompose(content -> new DigestFromContent(content).digest())
                            .thenCompose(
                                digest -> {
                                    final CompletionStage<Response> res;
                                    if (digest.string().equals(request.digest().string())) {
                                        res = upload.content().thenCompose(
                                            content -> repo.layers().put(content, digest)
                                                .thenCompose(
                                                    blob -> upload.delete().thenApply(
                                                        ignored -> Put.getResponse(name, digest)
                                                    )
                                                )
                                        );
                                    } else {
                                        res = CompletableFuture.completedStage(
                                            new RsWithStatus(RsStatus.BAD_REQUEST)
                                        );
                                    }
                                    return res;
                                }
                            )
                    ).orElseGet(
                        () -> CompletableFuture.completedStage(new RsWithStatus(RsStatus.NOT_FOUND))
                    )
                )
            );
        }

        /**
         * Returns response.
         * @param name Repo name
         * @param digest Digest
         * @return Response as CompletionStage
         */
        private static Response getResponse(
            final RepoName name, final Digest digest
        ) {
            return new RsWithHeaders(
                new RsWithStatus(RsStatus.CREATED),
                new Header(
                    "Location",
                    String.format(
                        "/v2/%s/blobs/%s",
                        name.value(),
                        digest.string()
                    )
                ),
                new Header("Content-Length", "0"),
                new DigestHeader(digest)
            );
        }
    }

    /**
     * Slice for GET method.
     *
     * @since 0.3
     */
    public static final class Get implements Slice {

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
            final String uuid = request.uuid();
            return new AsyncResponse(
                this.docker.repo(name).uploads().get(uuid).thenCompose(
                    found -> found.<CompletionStage<Response>>map(
                        upload -> upload.offset().thenApply(
                            offset -> new RsWithHeaders(
                                new RsWithStatus(RsStatus.NO_CONTENT),
                                new Header("Content-Length", "0"),
                                new Header("Range", String.format("0-%d", offset)),
                                new Header("Docker-Upload-UUID", uuid)
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
     * HTTP request to upload blob entity.
     *
     * @since 0.2
     */
    static final class Request {

        /**
         * RegEx pattern for path.
         */
        public static final Pattern QUERY = Pattern.compile("digest=(?<digest>[^=]*)");

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
         * Get upload UUID.
         *
         * @return Upload UUID.
         */
        String uuid() {
            return this.path().group("uuid");
        }

        /**
         * Get digest.
         *
         * @return Digest.
         */
        Digest digest() {
            final String query = Objects.requireNonNull(
                new RequestLineFrom(this.line).uri().getQuery(),
                String.format("No query in request: %s", this.line)
            );
            final Matcher matcher = QUERY.matcher(query);
            if (!matcher.matches()) {
                throw new IllegalStateException(String.format("Unexpected query: %s", query));
            }
            return new Digest.FromString(matcher.group("digest"));
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
     * Upload blob status HTTP response.
     *
     * @since 0.2
     */
    private static class StatusResponse implements Response {

        /**
         * Repository name.
         */
        private final RepoName name;

        /**
         * Upload UUID.
         */
        private final String uuid;

        /**
         * Current upload offset.
         */
        private final long offset;

        /**
         * Ctor.
         *
         * @param name Repository name.
         * @param uuid Upload UUID.
         * @param offset Current upload offset.
         */
        StatusResponse(final RepoName name, final String uuid, final long offset) {
            this.name = name;
            this.uuid = uuid;
            this.offset = offset;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            return new RsWithHeaders(
                new RsWithStatus(RsStatus.ACCEPTED),
                new Header(
                    "Location",
                    String.format("/v2/%s/blobs/uploads/%s", this.name.value(), this.uuid)
                ),
                new Header("Range", String.format("0-%d", this.offset)),
                new Header("Content-Length", "0"),
                new Header("Docker-Upload-UUID", this.uuid)
            ).send(connection);
        }
    }
}
