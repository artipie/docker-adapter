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
package com.artipie.docker;

import com.artipie.asto.Content;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

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
    CompletionStage<Long> append(Publisher<ByteBuffer> chunk);

    /**
     * Get uploaded content.
     *
     * @return Content.
     */
    CompletionStage<Content> content();

    /**
     * Get offset for the uploaded content.
     *
     * @return Offset.
     */
    CompletionStage<Long> offset();

    /**
     * Deletes upload blob data.
     *
     * @return Completion or error signal.
     */
    CompletionStage<Void> delete();
}
