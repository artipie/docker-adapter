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

import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;

/**
 * Path to blob resource.
 *
 * @since 0.3
 */
final class BlobPath {

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Blob digest.
     */
    private final Digest digest;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param digest Blob digest.
     */
    BlobPath(final RepoName name, final Digest digest) {
        this.name = name;
        this.digest = digest;
    }

    /**
     * Build path string.
     *
     * @return Path string.
     */
    public String string() {
        return String.format("/v2/%s/blobs/%s", this.name.value(), this.digest.string());
    }
}
