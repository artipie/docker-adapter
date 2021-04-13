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
import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import com.artipie.docker.error.InvalidDigestException;
import com.artipie.docker.misc.DigestedFlowable;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Asto implementation of {@link Upload}.
 *
 * @since 0.2
 */
public final class AstoUpload implements Upload {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Uploads layout.
     */
    private final UploadsLayout layout;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Upload UUID.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final String uuid;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param layout Uploads layout.
     * @param name Repository name.
     * @param uuid Upload UUID.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public AstoUpload(
        final Storage storage,
        final UploadsLayout layout,
        final RepoName name,
        final String uuid
    ) {
        this.storage = storage;
        this.layout = layout;
        this.name = name;
        this.uuid = uuid;
    }

    @Override
    public String uuid() {
        return this.uuid;
    }

    @Override
    public CompletionStage<Void> start() {
        return this.storage.save(this.started(), new Content.From(new byte[0]));
    }

    @Override
    public CompletionStage<Long> append(final Content chunk) {
        return this.chunks().thenCompose(
            chunks -> {
                if (chunks.size() > 0) {
                    throw new UnsupportedOperationException("Multiple chunks are not supported");
                }
                final Key tmp = new Key.From(this.root(), UUID.randomUUID().toString());
                final DigestedFlowable data = new DigestedFlowable(chunk);
                return this.storage.save(tmp, new Content.From(chunk.size(), data)).thenCompose(
                    nothing -> {
                        final Key key = this.chunk(data.digest());
                        return this.storage.move(tmp, key).thenApply(ignored -> key);
                    }
                ).thenCompose(
                    key -> this.storage.size(key).thenApply(updated -> updated - 1)
                );
            }
        );
    }

    @Override
    public CompletionStage<Long> offset() {
        return this.chunks().thenCompose(
            chunks -> {
                final CompletionStage<Long> result;
                if (chunks.isEmpty()) {
                    result = CompletableFuture.completedFuture(0L);
                } else {
                    final Key key = chunks.iterator().next();
                    result = this.storage.size(key).thenApply(size -> Math.max(size - 1, 0));
                }
                return result;
            }
        );
    }

    @Override
    public CompletionStage<Blob> putTo(final Layers layers, final Digest digest) {
        final Key source = this.chunk(digest);
        return this.storage.exists(source).thenCompose(
            exists -> {
                final CompletionStage<Blob> result;
                if (exists) {
                    result = layers.put(
                        new BlobSource() {
                            @Override
                            public Digest digest() {
                                return digest;
                            }

                            @Override
                            public CompletionStage<Void> saveTo(final Storage asto, final Key key) {
                                return asto.move(source, key);
                            }
                        }
                    ).thenCompose(
                        blob -> this.delete().thenApply(nothing -> blob)
                    );
                } else {
                    result = new FailedCompletionStage<>(
                        new InvalidDigestException(digest.toString())
                    );
                }
                return result;
            }
        );
    }

    /**
     * Root key for upload chunks.
     *
     * @return Root key.
     */
    Key root() {
        return this.layout.upload(this.name, this.uuid);
    }

    /**
     * Upload started marker key.
     *
     * @return Key.
     */
    private Key started() {
        return new Key.From(this.root(), "started");
    }

    /**
     * Build upload chunk key for given digest.
     *
     * @param digest Digest.
     * @return Chunk key.
     */
    private Key chunk(final Digest digest) {
        return new Key.From(this.root(), String.format("%s_%s", digest.alg(), digest.hex()));
    }

    /**
     * List all chunk keys.
     *
     * @return Chunk keys.
     */
    private CompletableFuture<Collection<Key>> chunks() {
        return this.storage.list(this.root()).thenApply(
            keys -> keys.stream()
                .filter(key -> !key.string().equals(this.started().string()))
                .collect(Collectors.toList())
        );
    }

    /**
     * Deletes upload blob data.
     *
     * @return Completion or error signal.
     */
    private CompletionStage<Void> delete() {
        return this.storage.list(this.root())
            .thenCompose(
                list -> CompletableFuture.allOf(
                    list.stream().map(file -> this.storage.delete(file).toCompletableFuture())
                        .toArray(CompletableFuture[]::new)
                )
            );
    }
}
