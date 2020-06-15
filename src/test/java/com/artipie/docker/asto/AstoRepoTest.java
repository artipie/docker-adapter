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

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoRepo}.
 *
 * @since 0.3
 */
final class AstoRepoTest {

    /**
     * Layers tested.
     */
    private Repo repo;

    @BeforeEach
    void setUp() {
        final InMemoryStorage storage = new InMemoryStorage();
        this.repo = new AstoRepo(storage, new AstoBlobs(storage), new RepoName.Valid("test"));
    }

    @Test
    void shouldCreateAstoLayers() {
        MatcherAssert.assertThat(
            this.repo.layers(),
            Matchers.instanceOf(AstoLayers.class)
        );
    }

    @Test
    void shouldCreateAstoManifests() {
        MatcherAssert.assertThat(
            this.repo.manifests(),
            Matchers.instanceOf(AstoManifests.class)
        );
    }
}
