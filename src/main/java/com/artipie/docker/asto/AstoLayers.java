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

package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Asto implementation of {@link Layers}.
 *
 * @since 0.3
 */
public final class AstoLayers implements Layers {

    /**
     * Blobs storage.
     */
    private final BlobStore blobs;

    /**
     * Ctor.
     *
     * @param blobs Blobs storage.
     */
    public AstoLayers(final BlobStore blobs) {
        this.blobs = blobs;
    }

    @Override
    public CompletionStage<Blob> put(final Content content, final Digest digest) {
        return this.blobs.put(content, digest);
    }

    @Override
    public CompletionStage<Optional<Blob>> get(final Digest digest) {
        return this.blobs.blob(digest);
    }
}
