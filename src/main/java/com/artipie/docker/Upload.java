/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.asto.Content;
import java.util.concurrent.CompletionStage;

/**
 * Blob upload.
 * See <a href="https://docs.docker.com/registry/spec/api/#blob-upload">Blob Upload</a>
 *
 * @since 0.2
 */
public interface Upload {

    /**
     * Read UUID.
     *
     * @return UUID.
     */
    String uuid();

    /**
     * Start upload.
     *
     * @return Completion or error signal.
     */
    CompletionStage<Void> start();

    /**
     * Appends a chunk of data to upload.
     *
     * @param chunk Chunk of data.
     * @return Offset after appending chunk.
     */
    CompletionStage<Long> append(Content chunk);

    /**
     * Get offset for the uploaded content.
     *
     * @return Offset.
     */
    CompletionStage<Long> offset();

    /**
     * Puts uploaded data to {@link Layers} creating a {@link Blob} with specified {@link Digest}.
     * If upload data mismatch provided digest then error occurs and operation does not complete.
     *
     * @param layers Target layers.
     * @param digest Expected blob digest.
     * @return Created blob.
     */
    CompletionStage<Blob> putTo(Layers layers, Digest digest);
}
