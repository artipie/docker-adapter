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
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.ByteBufPublisher;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoUpload}.
 *
 * @since 0.2
 */
class AstoUploadTest {

    /**
     * Slice being tested.
     */
    private AstoUpload upload;

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.upload = new AstoUpload(
            this.storage,
            new RepoName.Valid("test"),
            UUID.randomUUID().toString()
        );
    }

    @Test
    void shouldCreateDataOnStart() {
        this.upload.start().toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.list(this.upload.root()).join().isEmpty(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldReturnOffsetWhenAppendedChunk() {
        final byte[] chunk = "sample".getBytes();
        this.upload.start().toCompletableFuture().join();
        final Long offset = this.upload.append(Flowable.just(ByteBuffer.wrap(chunk)))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(offset, new IsEqual<>((long) chunk.length - 1));
    }

    @Test
    void shouldReadAppendedChunk() {
        final byte[] chunk = "chunk".getBytes();
        this.upload.start().toCompletableFuture().join();
        this.upload.append(Flowable.just(ByteBuffer.wrap(chunk))).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new ByteBufPublisher(this.contentFromUpload()).bytes().toCompletableFuture().join(),
            new IsEqual<>(chunk)
        );
    }

    @Test
    void shouldFailAppendedSecondChunk() {
        this.upload.start().toCompletableFuture().join();
        this.upload.append(Flowable.just(ByteBuffer.wrap("one".getBytes())))
            .toCompletableFuture()
            .join();
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                CompletionException.class,
                () -> this.upload.append(Flowable.just(ByteBuffer.wrap("two".getBytes())))
                    .toCompletableFuture()
                    .join()
            ).getCause(),
            new IsInstanceOf(UnsupportedOperationException.class)
        );
    }

    @Test
    void shouldAppendedSecondChunkIfFirstOneFailed() {
        this.upload.start().toCompletableFuture().join();
        try {
            this.upload.append(Flowable.error(new IllegalStateException()))
                .toCompletableFuture()
                .join();
        } catch (final CompletionException ignored) {
        }
        final byte[] chunk = "content".getBytes();
        this.upload.append(Flowable.just(ByteBuffer.wrap(chunk))).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new ByteBufPublisher(this.contentFromUpload()).bytes().toCompletableFuture().join(),
            new IsEqual<>(chunk)
        );
    }

    @Test
    void shouldRemoveUploadedFiles() throws ExecutionException, InterruptedException {
        this.upload.start().toCompletableFuture().join();
        final byte[] chunk = "some bytes".getBytes();
        this.upload.append(Flowable.just(ByteBuffer.wrap(chunk))).toCompletableFuture().get();
        this.upload.delete().toCompletableFuture().get();
        MatcherAssert.assertThat(
            this.storage.list(this.upload.root()).get(),
            new IsEmptyCollection<>()
        );
    }

    /**
     * Gets content from upload.
     * @return Content
     */
    private Content contentFromUpload() {
        return this.upload.content().toCompletableFuture().join();
    }
}
