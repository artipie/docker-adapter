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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Asto {@link Docker} implementation.
 * @since 0.1
 */
public final class AstoDocker implements Docker {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Storage layout.
     */
    private final Layout layout;

    /**
     * Ctor.
     * @param asto Asto storage
     */
    public AstoDocker(final Storage asto) {
        this(asto, new DefaultLayout());
    }

    /**
     * Ctor.
     *
     * @param asto Storage.
     * @param layout Storage layout.
     */
    public AstoDocker(final Storage asto, final Layout layout) {
        this.asto = asto;
        this.layout = layout;
    }

    @Override
    public Repo repo(final RepoName name) {
        return new AstoRepo(this.asto, this.layout, name);
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        final Key root = this.layout.repositories();
        return this.asto.list(root).thenApply(keys -> new AstoCatalog(root, keys, from, limit));
    }
}
