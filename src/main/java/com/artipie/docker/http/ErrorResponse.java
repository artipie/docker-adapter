package com.artipie.docker.http;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

/**
 * Docker response in case of errors.
 *
 * @since 0.2
 */
public class ErrorResponse implements Response {
    /**
     * Origin response.
     */
    private final Response origin;

    /**
     * Ctor.
     * @param origin Origin response
     */
    ErrorResponse(final Response origin) {
        this.origin = origin;
    }

    /**
     * Ctor.
     * @param status Http status
     * @param buff Origin response
     */
    ErrorResponse(RsStatus status, final ByteBuffer buff) {
        this.origin = new RsWithBody(
            new RsWithHeaders(
                new RsWithStatus(status),
                new Headers.From("Content-Type", "application/json")
            ),
            buff
        );
    }

    /**
     * Ctor.
     * @param code Code of response
     * @param message Message of response
     * @param detail More detail about of  response
     */
    ErrorResponse(RsStatus status, String code, String message, String detail) {
        this(
            status,
            ByteBuffer.wrap("{ \"errors:\" [{\r\n            \"code\": ,\r\n            \"message\": %s,\\r\\n            \"detail\": %s\r\n        }\r\n    ]\r\n}".getBytes())
        );
    }

    @Override
    public CompletionStage<Void> send(final Connection connection) {
        return this.origin.send(connection);
    }
}

