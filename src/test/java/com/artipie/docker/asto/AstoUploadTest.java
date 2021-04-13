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
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import io.reactivex.Flowable;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
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
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
            new DefaultLayout(),
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
        final Long offset = this.upload.append(new Content.From(chunk))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(offset, new IsEqual<>((long) chunk.length - 1));
    }

    @Test
    void shouldReadAppendedChunk() {
        final byte[] chunk = "chunk".getBytes();
        this.upload.start().toCompletableFuture().join();
        this.upload.append(new Content.From(chunk)).toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.upload,
            new IsUploadWithContent(chunk)
        );
    }

    @Test
    void shouldFailAppendedSecondChunk() {
        this.upload.start().toCompletableFuture().join();
        this.upload.append(new Content.From("one".getBytes()))
            .toCompletableFuture()
            .join();
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                CompletionException.class,
                () -> this.upload.append(new Content.From("two".getBytes()))
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
            this.upload.append(new Content.From(1, Flowable.error(new IllegalStateException())))
                .toCompletableFuture()
                .join();
        } catch (final CompletionException ignored) {
        }
        final byte[] chunk = "content".getBytes();
        this.upload.append(new Content.From(chunk)).toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.upload,
            new IsUploadWithContent(chunk)
        );
    }

    @Test
    void shouldRemoveUploadedFiles() throws ExecutionException, InterruptedException {
        this.upload.start().toCompletableFuture().join();
        final byte[] chunk = "some bytes".getBytes();
        this.upload.append(new Content.From(chunk)).toCompletableFuture().get();
        this.upload.putTo(new CapturePutLayers(), new Digest.Sha256(chunk))
            .toCompletableFuture().get();
        MatcherAssert.assertThat(
            this.storage.list(this.upload.root()).get(),
            new IsEmptyCollection<>()
        );
    }

    /**
     * Matcher for {@link Upload} content.
     *
     * @since 0.12
     */
    private final class IsUploadWithContent extends TypeSafeMatcher<Upload> {

        /**
         * Expected content.
         */
        private final byte[] content;

        private IsUploadWithContent(final byte[] content) {
            this.content = Arrays.copyOf(content, content.length);
        }

        @Override
        public void describeTo(final Description description) {
            new IsEqual<>(this.content).describeTo(description);
        }

        @Override
        public boolean matchesSafely(final Upload upl) {
            final Digest digest = new Digest.Sha256(this.content);
            final CapturePutLayers fake = new CapturePutLayers();
            upl.putTo(fake, digest).toCompletableFuture().join();
            return new IsEqual<>(this.content).matches(fake.content());
        }
    }

    /**
     * Layers implementation that captures put method content.
     *
     * @since 0.12
     */
    private final class CapturePutLayers implements Layers {

        /**
         * Captured put content.
         */
        private volatile byte[] ccontent;

        @Override
        public CompletionStage<Blob> put(final BlobSource source) {
            final Key key = new Key.From(UUID.randomUUID().toString());
            source.saveTo(AstoUploadTest.this.storage, key).toCompletableFuture().join();
            this.ccontent = AstoUploadTest.this.storage.value(key)
                .thenApply(PublisherAs::new)
                .thenCompose(PublisherAs::bytes)
                .toCompletableFuture().join();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Blob> mount(final Blob blob) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Optional<Blob>> get(final Digest digest) {
            throw new UnsupportedOperationException();
        }

        public byte[] content() {
            return this.ccontent;
        }
    }
}
