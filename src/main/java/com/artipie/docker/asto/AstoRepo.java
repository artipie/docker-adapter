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

import com.artipie.asto.Storage;
import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Uploads;

/**
 * Asto implementation of {@link Repo}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoRepo implements Repo {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Blobs storage.
     */
    private final BlobStore blobs;

    /**
     * Ctor.
     *
     * @param asto Asto storage
     * @param blobs Blobs storage.
     * @param name Repository name
     */
    public AstoRepo(final Storage asto, final BlobStore blobs, final RepoName name) {
        this.asto = asto;
        this.blobs = blobs;
        this.name = name;
    }

    @Override
    public Layers layers() {
        return new AstoLayers(this.blobs);
    }

    @Override
    public Manifests manifests() {
        return new AstoManifests(this.asto, this.blobs, this.name);
    }

    @Override
    public Uploads uploads() {
        return new AstoUploads(this.asto, this.name);
    }
}
