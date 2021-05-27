/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * Tests for {@link AuthScopeSlice}.
 *
 * @since 0.11
 */
class AuthScopeSliceTest {

    @Test
    void testScope() {
        final String line = "GET /resource.txt HTTP/1.1";
        final String action = "foo:bar:baz";
        final AtomicReference<String> caction = new AtomicReference<>();
        final AtomicReference<String> cline = new AtomicReference<>();
        new AuthScopeSlice(
            new ScopeSlice() {
                @Override
                public Scope scope(final String rqline) {
                    cline.set(rqline);
                    return new Scope.FromString(action);
                }

                @Override
                public Response response(
                    final String line,
                    final Iterable<Map.Entry<String, String>> headers,
                    final Publisher<ByteBuffer> body
                ) {
                    return StandardRs.OK;
                }
            },
            (AuthScheme) headers -> CompletableFuture.completedFuture(
                new AuthScheme.Result() {
                    @Override
                    public Optional<Authentication.User> user() {
                        return Optional.of(new Authentication.User("alice"));
                    }

                    @Override
                    public String challenge() {
                        return "";
                    }
                }
            ),
            (user, act) -> {
                caction.set(act);
                return true;
            }
        ).response(line, Headers.EMPTY, Content.EMPTY).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Request line passed to slice",
            cline.get(),
            new IsEqual<>(line)
        );
        MatcherAssert.assertThat(
            "Scope passed as action to permissions",
            caction.get(),
            new IsEqual<>(action)
        );
    }
}
