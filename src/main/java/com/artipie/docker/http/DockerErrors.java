package com.artipie.docker.http;

import com.artipie.http.Connection;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import java.util.concurrent.CompletionStage;

/**
 * Docker response in case of errors.
 *
 * @since 0.2
 */
enum DockerErrors implements Response {
    /**
     * internal error with json.
     */
    BLOB_UNKNOWN(
        new ErrorResponse(
            RsStatus.BAD_REQUEST,
                "BLOB_UNKNOWN",
                "blob unknown to registry",
                ""
        )
    ),

    /**
     * internal error with json.
     */
    MANIFEST_INVALID(
        new ErrorResponse(
            RsStatus.BAD_REQUEST,
                "MANIFEST_INVALID",
                "manifest invalid",
                ""
        )
    );
    /**
     * Origin response.
     */
    private final Response origin;

    /**
     * Ctor.
     * @param origin Origin response
     */
    DockerErrors(final Response origin) {
        this.origin = origin;
    }

    @Override
    public CompletionStage<Void> send(final Connection connection) {
        return this.origin.send(connection);
    }
}
