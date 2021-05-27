/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthSlice;
import com.artipie.http.auth.Permissions;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice that implements authorization for {@link ScopeSlice}.
 *
 * @since 0.11
 */
final class AuthScopeSlice implements Slice {

    /**
     * Origin.
     */
    private final ScopeSlice origin;

    /**
     * Authentication scheme.
     */
    private final AuthScheme auth;

    /**
     * Access permissions.
     */
    private final Permissions perms;

    /**
     * Ctor.
     *
     * @param origin Origin slice.
     * @param auth Authentication scheme.
     * @param perms Access permissions.
     */
    AuthScopeSlice(
        final ScopeSlice origin,
        final AuthScheme auth,
        final Permissions perms
    ) {
        this.origin = origin;
        this.auth = auth;
        this.perms = perms;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AuthSlice(
            this.origin,
            this.auth,
            user -> this.perms.allowed(user, this.origin.scope(line).string())
        ).response(line, headers, body);
    }
}
