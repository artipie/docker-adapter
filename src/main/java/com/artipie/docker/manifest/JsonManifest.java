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
package com.artipie.docker.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.misc.Json;
import java.util.Collection;
import java.util.concurrent.CompletionStage;

/**
 * Image manifest in JSON format.
 *
 * @since 0.2
 */
public final class JsonManifest implements Manifest {

    /**
     * JSON bytes.
     */
    private final Content source;

    /**
     * Ctor.
     *
     * @param source JSON bytes.
     */
    public JsonManifest(final Content source) {
        this.source = source;
    }

    @Override
    public CompletionStage<String> mediaType() {
        return new Json(this.source).object().thenApply(root -> root.getString("mediaType"));
    }

    @Override
    public CompletionStage<Manifest> convert(final Collection<String> options) {
        return this.mediaType().thenApply(
            type -> {
                if (!options.contains(type)) {
                    throw new IllegalArgumentException(
                        String.format("Cannot convert from '%s' to any of '%s'", type, options)
                    );
                }
                return this;
            }
        );
    }

    @Override
    public Content content() {
        return this.source;
    }
}
