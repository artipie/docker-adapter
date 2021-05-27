/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.TrustedBlobSource;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.BearerAuthScheme;
import com.artipie.http.auth.Permissions;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link DockerSlice}.
 * Authentication & authorization tests.
 *
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @todo #434:30min test `shouldReturnForbiddenWhenUserHasNoRequiredPermissionOnSecondManifestPut`
 *  fails in github actions, locally it works fine. Figure out what is the problem and fix it.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class AuthTest {

    /**
     * Docker used in tests.
     */
    private Docker docker;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldReturnUnauthorizedWhenNoAuth(final Method method, final RequestLine line) {
        MatcherAssert.assertThat(
            method.slice("whatever").response(line.toString(), Headers.EMPTY, Content.EMPTY),
            new IsUnauthorizedResponse()
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldReturnUnauthorizedWhenUserIsUnknown(final Method method, final RequestLine line) {
        MatcherAssert.assertThat(
            method.slice("whatever").response(
                line.toString(),
                method.headers(new TestAuthentication.User("chuck", "letmein")),
                Content.EMPTY
            ),
            new IsUnauthorizedResponse()
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldReturnForbiddenWhenUserHasNoRequiredPermissions(
        final Method method,
        final RequestLine line,
        final String action
    ) {
        MatcherAssert.assertThat(
            method.slice(action).response(
                line.toString(),
                method.headers(TestAuthentication.BOB),
                Content.EMPTY
            ),
            new IsDeniedResponse()
        );
    }

    @Test
    @Disabled
    void shouldReturnForbiddenWhenUserHasNoRequiredPermissionOnSecondManifestPut() {
        final Basic basic = new Basic(this.docker);
        final String line = new RequestLine(RqMethod.PUT, "/v2/my-alpine/manifests/latest")
            .toString();
        final String action = "repository:my-alpine:push";
        basic.slice(action).response(
            line,
            basic.headers(TestAuthentication.ALICE),
            this.manifest()
        );
        MatcherAssert.assertThat(
            basic.slice(action).response(
                line,
                basic.headers(TestAuthentication.ALICE),
                Content.EMPTY
            ),
            new RsHasStatus(RsStatus.FORBIDDEN)
        );
    }

    @Test
    void shouldOverwriteManifestIfAllowed() {
        final Basic basic = new Basic(this.docker);
        final String path = "/v2/my-alpine/manifests/abc";
        final String line = new RequestLine(RqMethod.PUT, path).toString();
        final String action = "repository:my-alpine:overwrite";
        final Flowable<ByteBuffer> manifest = this.manifest();
        MatcherAssert.assertThat(
            "Manifest was created for the first time",
            basic.slice(action).response(
                line,
                basic.headers(TestAuthentication.ALICE),
                manifest
            ),
            new ResponseMatcher(
                RsStatus.CREATED,
                new Header("Location", path),
                new Header("Content-Length", "0"),
                new Header(
                    "Docker-Content-Digest",
                    "sha256:ef0ff2adcc3c944a63f7cafb386abc9a1d95528966085685ae9fab2a1c0bedbf"
                )
            )
        );
        MatcherAssert.assertThat(
            "Manifest was overwritten",
            basic.slice(action).response(
                line,
                basic.headers(TestAuthentication.ALICE),
                manifest
            ),
            new ResponseMatcher(
                RsStatus.CREATED,
                new Header("Location", path),
                new Header("Content-Length", "0"),
                new Header(
                    "Docker-Content-Digest",
                    "sha256:ef0ff2adcc3c944a63f7cafb386abc9a1d95528966085685ae9fab2a1c0bedbf"
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldNotReturnUnauthorizedOrForbiddenWhenUserHasPermissions(
        final Method method,
        final RequestLine line,
        final String action
    ) {
        final Response response = method.slice(action).response(
            line.toString(),
            method.headers(TestAuthentication.ALICE),
            Content.EMPTY
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new IsNot<>(new RsHasStatus(RsStatus.FORBIDDEN)),
                    new IsNot<>(new RsHasStatus(RsStatus.UNAUTHORIZED))
                )
            )
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Arguments> setups() {
        return Stream.of(new Basic(), new Bearer()).flatMap(AuthTest::setups);
    }

    /**
     * Create manifest content.
     *
     * @return Manifest content.
     */
    private Flowable<ByteBuffer> manifest() {
        final byte[] content = "config".getBytes();
        final Blob config = this.docker.repo(new RepoName.Valid("my-alpine")).layers()
            .put(new TrustedBlobSource(content))
            .toCompletableFuture().join();
        final byte[] data = String.format(
            "{\"config\":{\"digest\":\"%s\"},\"layers\":[],\"mediaType\":\"my-type\"}",
            config.digest().string()
        ).getBytes();
        return Flowable.just(ByteBuffer.wrap(data));
    }

    private static Stream<Arguments> setups(final Method method) {
        return Stream.of(
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/"),
                "registry:base:*"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.HEAD, "/v2/my-alpine/manifests/1"),
                "repository:my-alpine:pull"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/my-alpine/manifests/2"),
                "repository:my-alpine:pull"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.PUT, "/v2/my-alpine/manifests/latest"),
                "repository:my-alpine:push"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.PUT, "/v2/my-alpine/manifests/latest"),
                "repository:my-alpine:overwrite"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/my-alpine/tags/list"),
                "repository:my-alpine:pull"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.HEAD, "/v2/my-alpine/blobs/sha256:123"),
                "repository:my-alpine:pull"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/my-alpine/blobs/sha256:012345"),
                "repository:my-alpine:pull"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.POST, "/v2/my-alpine/blobs/uploads/"),
                "repository:my-alpine:push"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.PATCH, "/v2/my-alpine/blobs/uploads/123"),
                "repository:my-alpine:push"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.PUT, "/v2/my-alpine/blobs/uploads/12345"),
                "repository:my-alpine:push"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/my-alpine/blobs/uploads/112233"),
                "repository:my-alpine:pull"
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/_catalog"),
                "registry:catalog:*"
            )
        );
    }

    /**
     * Authentication method.
     *
     * @since 0.8
     */
    private interface Method {

        Slice slice(String action);

        Headers headers(TestAuthentication.User user);

    }

    /**
     * Basic authentication method.
     *
     * @since 0.8
     */
    private static final class Basic implements Method {

        /**
         * Docker repo.
         */
        private final Docker docker;

        private Basic(final Docker docker) {
            this.docker = docker;
        }

        private Basic() {
            this(new AstoDocker(new InMemoryStorage()));
        }

        @Override
        public Slice slice(final String action) {
            return new DockerSlice(
                this.docker,
                new Permissions.Single(TestAuthentication.ALICE.name(), action),
                new BasicAuthScheme(new TestAuthentication())
            );
        }

        @Override
        public Headers headers(final TestAuthentication.User user) {
            return user.headers();
        }

        @Override
        public String toString() {
            return "Basic";
        }
    }

    /**
     * Bearer authentication method.
     *
     * @since 0.8
     */
    private static final class Bearer implements Method {

        @Override
        public Slice slice(final String action) {
            return new DockerSlice(
                new AstoDocker(new InMemoryStorage()),
                new Permissions.Single(TestAuthentication.ALICE.name(), action),
                new BearerAuthScheme(
                    token -> CompletableFuture.completedFuture(
                        Stream.of(TestAuthentication.ALICE, TestAuthentication.BOB)
                            .filter(user -> token.equals(token(user)))
                            .map(user -> new Authentication.User(user.name()))
                            .findFirst()
                    ),
                    ""
                )
            );
        }

        @Override
        public Headers headers(final TestAuthentication.User user) {
            return new Headers.From(
                new Authorization.Bearer(token(user))
            );
        }

        @Override
        public String toString() {
            return "Bearer";
        }

        private static String token(final TestAuthentication.User user) {
            return String.format("%s:%s", user.name(), user.password());
        }
    }
}
