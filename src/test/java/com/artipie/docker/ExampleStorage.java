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

import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Storage with example docker repository data from resources folder 'example-my-alpine'.
 *
 * @since 0.2
 */
public final class ExampleStorage extends Storage.Wrap {

    /**
     * Ctor.
     */
    public ExampleStorage() {
        super(new FileStorage(path()));
    }

    /**
     * Path to example storage files.
     *
     * @return Files path.
     */
    private static Path path() {
        try {
            return Path.of(
                Thread.currentThread().getContextClassLoader()
                    .getResource("example-my-alpine").toURI()
            );
        } catch (final URISyntaxException ex) {
            throw new IllegalStateException("Failed to resolve resources path", ex);
        }
    }
}
