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
package com.artipie.docker.proxy;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Slice augmenting requests with Authorization header when needed.
 *
 * @since 0.3
 */
public final class AuthClientSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Ctor.
     *
     * @param origin Origin slice.
     */
    public AuthClientSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final CompletableFuture<Response> promise = new CompletableFuture<>();
        return new AsyncResponse(
            this.origin.response(line, headers, body).send(
                (rsstatus, rsheaders, rsbody) -> {
                    final RsFull original = new RsFull(rsstatus, rsheaders, rsbody);
                    final Response response;
                    if (rsstatus == RsStatus.UNAUTHORIZED) {
                        response = new AsyncResponse(
                            this.authenticate(new WwwAuthenticate(rsheaders)).thenApply(
                                authorization -> this.origin.response(
                                    line,
                                    new Headers.From(headers, authorization),
                                    body
                                )
                            )
                        );
                    } else {
                        response = original;
                    }
                    promise.complete(response);
                    return CompletableFuture.allOf();
                }
            ).thenCompose(nothing -> promise)
        );
    }

    /**
     * Create Authorization header for given WWW-Authenticate header.
     *
     * @param header WWW-Authenticate header.
     * @return Authorization header.
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private CompletionStage<Map.Entry<String, String>> authenticate(final WwwAuthenticate header) {
        throw new UnsupportedOperationException();
    }
}