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

import com.artipie.asto.Concatenation;
import com.artipie.asto.Remaining;
import com.artipie.docker.ExampleStorage;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link AstoRepo}.
 * @since 0.1
 */
final class AstoRepoITCase {

    /**
     * Repository being tested.
     */
    private Repo repo;

    @BeforeEach
    void setUp() {
        this.repo = new AstoRepo(
            new ExampleStorage(),
            new RepoName.Simple("my-alpine")
        );
    }

    @Test
    void shouldReadManifest() throws Exception {
        final ManifestRef ref = new ManifestRef.FromTag(new Tag.Valid("1"));
        final byte[] manifest = this.repo.manifest(ref)
            .thenApply(Optional::get)
            .thenApply(Manifest::content)
            .thenApply(Concatenation::new)
            .thenCompose(c -> c.single().to(SingleInterop.get()))
            .thenApply(Remaining::new)
            .thenApply(Remaining::bytes)
            .toCompletableFuture()
            .get();
        // @checkstyle MagicNumberCheck (1 line)
        MatcherAssert.assertThat(manifest.length, Matchers.equalTo(528));
    }

    @Test
    void shouldReadNoManifestIfAbsent() throws Exception {
        final Optional<Manifest> manifest = this.repo.manifest(
            new ManifestRef.FromTag(new Tag.Valid("2"))
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(manifest.isPresent(), new IsEqual<>(false));
    }
}
