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
import java.util.Collection;
import java.util.concurrent.CompletionStage;

/**
 * Image manifest.
 * See <a href="https://docs.docker.com/engine/reference/commandline/manifest/">docker manifest</a>
 *
 * @since 0.2
 */
public interface Manifest {

    /**
     * Read manifest type.
     *
     * @return Type string.
     */
    CompletionStage<String> mediaType();

    /**
     * Converts manifest to one of types.
     *
     * @param options Types the manifest may be converted to.
     * @return Converted manifest.
     */
    CompletionStage<Manifest> convert(Collection<String> options);

    /**
     * Read manifest binary content.
     *
     * @return Manifest binary content.
     */
    Content content();
}
