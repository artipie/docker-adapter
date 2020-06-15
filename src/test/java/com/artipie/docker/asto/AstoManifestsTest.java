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
import com.artipie.asto.Storage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.ExampleStorage;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.ByteBufPublisher;
import com.artipie.docker.ref.ManifestRef;
import java.util.Optional;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link AstoManifests}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class AstoManifestsTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Repository manifests being tested.
     */
    private AstoManifests manifests;

    @BeforeEach
    void setUp() {
        this.storage = new ExampleStorage();
        this.manifests = new AstoManifests(
            this.storage,
            new AstoBlobs(this.storage),
            new RepoName.Simple("my-alpine")
        );
    }

    @Test
    @Timeout(5)
    void shouldReadManifest() {
        final ManifestRef ref = new ManifestRef.FromTag(new Tag.Valid("1"));
        final byte[] manifest = this.manifest(ref);
        // @checkstyle MagicNumberCheck (1 line)
        MatcherAssert.assertThat(manifest.length, Matchers.equalTo(528));
    }

    @Test
    @Timeout(5)
    void shouldReadNoManifestIfAbsent() throws Exception {
        final Optional<Manifest> manifest = this.manifests.get(
            new ManifestRef.FromTag(new Tag.Valid("2"))
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(manifest.isPresent(), new IsEqual<>(false));
    }

    @Test
    @Timeout(5)
    void shouldReadAddedManifest() {
        final byte[] conf = "config".getBytes();
        final Blob config = new AstoBlobs(this.storage)
            .put(new Content.From(conf), new Digest.Sha256(conf)).toCompletableFuture().join();
        final byte[] lyr = "layer".getBytes();
        final Blob layer = new AstoBlobs(this.storage)
            .put(new Content.From(lyr), new Digest.Sha256(lyr)).toCompletableFuture().join();
        final byte[] data = Json.createObjectBuilder()
            .add(
                "config",
                Json.createObjectBuilder().add("digest", config.digest().string())
            )
            .add(
                "layers",
                Json.createArrayBuilder()
                    .add(
                        Json.createObjectBuilder().add("digest", layer.digest().string())
                    )
                    .add(
                        Json.createObjectBuilder()
                            .add("digest", "sha256:123")
                            .add("urls", Json.createArrayBuilder().add("https://artipie.com/"))
                    )
            )
            .build().toString().getBytes();
        final ManifestRef ref = new ManifestRef.FromTag(new Tag.Valid("some-tag"));
        final Manifest manifest = this.manifests.put(ref, new Content.From(data))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(this.manifest(ref), new IsEqual<>(data));
        MatcherAssert.assertThat(
            this.manifest(new ManifestRef.FromDigest(manifest.digest())),
            new IsEqual<>(data)
        );
    }

    private byte[] manifest(final ManifestRef ref) {
        return this.manifests.get(ref)
            .thenCompose(
                opt -> opt.map(mnf -> new ByteBufPublisher(mnf.content()).bytes()).orElseThrow()
            ).toCompletableFuture().join();
    }
}
