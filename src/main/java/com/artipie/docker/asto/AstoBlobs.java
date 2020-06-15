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
import com.artipie.asto.fs.RxFile;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Asto {@link BlobStore} implementation.
 * @since 0.1
 * @todo #41:30min Refactor this class, make it more readable.
 *  Put method is overcomplicated right now, try to decompose it,
 *  move some logic into new classes or methods.
 * @checkstyle ReturnCountCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class AstoBlobs implements BlobStore {

    /**
     * Storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Storage
     */
    AstoBlobs(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public CompletionStage<Optional<Blob>> blob(final Digest digest) {
        return this.asto.exists(new BlobKey(digest)).thenApply(
            exists -> {
                final Optional<Blob> blob;
                if (exists) {
                    blob = Optional.of(new AstoBlob(this.asto, digest));
                } else {
                    blob = Optional.empty();
                }
                return blob;
            }
        );
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletionStage<Blob> put(final Content blob, final Digest digest) {
        final Path tmp;
        final FileChannel out;
        try {
            tmp = Files.createTempFile(this.getClass().getSimpleName(), ".blob.tmp");
            out = FileChannel.open(tmp, StandardOpenOption.WRITE);
        } catch (final IOException err) {
            return CompletableFuture.failedFuture(err);
        }
        return Flowable.fromPublisher(blob)
            .flatMapCompletable(
                buf -> Completable.fromAction(
                    () -> {
                        while (buf.hasRemaining()) {
                            out.write(buf);
                        }
                    })
            )
            .doOnTerminate(out::close)
            .andThen(Single.just(out))
            .flatMap(
                ignored -> {
                    final RxFile file = new RxFile(tmp);
                    return file.size().flatMap(
                        size -> SingleInterop.fromFuture(
                            this.asto.save(
                                new BlobKey(digest),
                                new Content.From(size, file.flow())
                            ).<Blob>thenApply(empty -> new AstoBlob(this.asto, digest))
                        )
                    );
                }
            )
            .doAfterTerminate(() -> Files.delete(tmp))
            .to(SingleInterop.get()).toCompletableFuture();
    }
}
