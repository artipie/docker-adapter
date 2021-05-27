/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.error.ManifestError;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.AcceptHeader;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.docker.ref.ManifestRef;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Location;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
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
        "^/v2/(?<name>.*)/manifests/(?<reference>.*)$"
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
    public static class Head implements ScopeSlice {

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
        public Scope scope(final String line) {
            return new Scope.Repository.Pull(new Request(line).name());
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
            final Request request = new Request(line);
            final ManifestRef ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(request.name()).manifests().get(ref).thenApply(
                    manifest -> manifest.<Response>map(
                        found -> new RsWithHeaders(
                            new BaseResponse(found.convert(Head.acceptHeader(headers))),
                            new ContentLength(found.size())
                        )
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new ManifestError(ref))
                    )
                )
            );
        }

        /**
         * Gets accept header.
         * @param headers Request headers
         * @return Accept headers
         */
        private static Set<String> acceptHeader(final Iterable<Map.Entry<String, String>> headers) {
            return new AcceptHeader(headers).values();
        }
    }

    /**
     * Slice for GET method, getting manifest content.
     *
     * @since 0.2
     */
    public static class Get implements ScopeSlice {

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
        public Scope scope(final String line) {
            return new Scope.Repository.Pull(new Request(line).name());
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
                this.docker.repo(name).manifests().get(ref).thenApply(
                    manifest -> manifest.<Response>map(
                        found -> {
                            final Manifest mnf = found.convert(Head.acceptHeader(headers));
                            return new RsWithBody(new BaseResponse(mnf), mnf.content());
                        }
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new ManifestError(ref))
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
    public static class Put implements ScopeSlice {

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
        public Scope scope(final String line) {
            return new Scope.Repository.Push(new Request(line).name());
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
                        new Location(
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
     * Auth slice for PUT method, checks whether overwrite is allowed.
     *
     * @since 0.12
     */
    public static class PutAuth implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Origin.
         */
        private final ScopeSlice origin;

        /**
         * Access permissions.
         */
        private final Permissions perms;

        /**
         * Authentication scheme.
         */
        private final AuthScheme auth;

        /**
         * Ctor.
         * @param docker Docker
         * @param origin Origin slice
         * @param auth Authentication
         * @param perms Permission
         * @checkstyle ParameterNumberCheck (4 lines)
         */
        PutAuth(final Docker docker, final ScopeSlice origin,
            final AuthScheme auth, final Permissions perms) {
            this.docker = docker;
            this.origin = origin;
            this.perms = perms;
            this.auth = auth;
        }

        @Override
        public Response response(
            final String line, final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final RepoName name = request.name();
            final ManifestRef ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().get(ref).thenApply(
                    manifest -> {
                        final Permission perm;
                        if (manifest.isPresent()) {
                            perm = user -> this.perms.allowed(user, this.scope(line).string());
                        } else {
                            perm = user -> this.perms.allowed(
                                user, this.origin.scope(line).string()
                            ) || this.perms.allowed(user, this.scope(line).string());
                        }
                        return new DockerAuthSlice(
                            new AuthSlice(
                                this.origin,
                                this.auth,
                                perm
                            )
                        ).response(line, headers, body);
                    }
                )
            );
        }

        @Override
        public Scope scope(final String line) {
            return new Scope.Repository.OverwriteTags(new Request(line).name());
        }
    }

    /**
     * HTTP request to manifest entity.
     *
     * @since 0.2
     */
    static final class Request {

        /**
         * HTTP request by RegEx.
         */
        private final RqByRegex rqregex;

        /**
         * Ctor.
         *
         * @param line HTTP request line.
         */
        Request(final String line) {
            this.rqregex = new RqByRegex(line, ManifestEntity.PATH);
        }

        /**
         * Get repository name.
         *
         * @return Repository name.
         */
        RepoName name() {
            return new RepoName.Valid(this.rqregex.path().group("name"));
        }

        /**
         * Get manifest reference.
         *
         * @return Manifest reference.
         */
        ManifestRef reference() {
            return new ManifestRef.FromString(this.rqregex.path().group("reference"));
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
                new RsWithHeaders(
                    StandardRs.EMPTY,
                    new ContentType(mnf.mediaType()),
                    new DigestHeader(mnf.digest())
                )
            );
        }

    }
}
