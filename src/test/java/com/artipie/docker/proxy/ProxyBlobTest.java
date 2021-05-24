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

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProxyBlob}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class ProxyBlobTest {

    @Test
    void shouldReadContent() {
        final byte[] data = "data".getBytes();
        final Content content = new ProxyBlob(
            (line, headers, body) -> {
                if (!line.startsWith("GET /v2/test/blobs/sha256:123 ")) {
                    throw new IllegalArgumentException();
                }
                return new RsFull(
                    RsStatus.OK,
                    Headers.EMPTY,
                    new Content.From(data)
                );
            },
            new RepoName.Valid("test"),
            new Digest.FromString("sha256:123"),
            data.length
        ).content().toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(content).bytes().toCompletableFuture().join(),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            content.size(),
            new IsEqual<>(Optional.of((long) data.length))
        );
    }

    @Test
    void shouldReadSize() {
        final long size = 1235L;
        final ProxyBlob blob = new ProxyBlob(
            (line, headers, body) -> {
                throw new UnsupportedOperationException();
            },
            new RepoName.Valid("my/test"),
            new Digest.FromString("sha256:abc"),
            size
        );
        MatcherAssert.assertThat(
            blob.size().toCompletableFuture().join(),
            new IsEqual<>(size)
        );
    }

    @Test
    void shouldNotFinishSendWhenContentReceived() {
        final AtomicReference<CompletionStage<Void>> capture = new AtomicReference<>();
        this.captureConnectionAccept(capture, false);
        MatcherAssert.assertThat(
            capture.get().toCompletableFuture().isDone(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldFinishSendWhenContentConsumed() {
        final AtomicReference<CompletionStage<Void>> capture = new AtomicReference<>();
        final Content content = this.captureConnectionAccept(capture, false);
        new PublisherAs(content).bytes().toCompletableFuture().join();
        MatcherAssert.assertThat(
            capture.get().toCompletableFuture().isDone(),
            new IsEqual<>(true)
        );
    }

    @Test
    @SuppressWarnings("PMD.EmptyCatchBlock")
    void shouldFinishSendWhenContentIsBad() {
        final AtomicReference<CompletionStage<Void>> capture = new AtomicReference<>();
        final Content content = this.captureConnectionAccept(capture, true);
        try {
            new PublisherAs(content).bytes().toCompletableFuture().join();
        } catch (final CompletionException ex) {
        }
        MatcherAssert.assertThat(
            capture.get().toCompletableFuture().isDone(),
            new IsEqual<>(true)
        );
    }

    private Content captureConnectionAccept(
        final AtomicReference<CompletionStage<Void>> capture,
        final boolean failure
    ) {
        final byte[] data = "1234".getBytes();
        return new ProxyBlob(
            (line, headers, body) -> connection -> {
                final Content content;
                if (failure) {
                    content = new Content.From(Flowable.error(new IllegalStateException()));
                } else {
                    content = new Content.From(data);
                }
                final CompletionStage<Void> accept = connection.accept(
                    RsStatus.OK,
                    new Headers.From(new ContentLength(String.valueOf(data.length))),
                    content
                );
                capture.set(accept);
                return accept;
            },
            new RepoName.Valid("abc"),
            new Digest.FromString("sha256:987"),
            data.length
        ).content().toCompletableFuture().join();
    }
}
