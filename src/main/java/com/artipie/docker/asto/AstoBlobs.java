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
import com.artipie.asto.fs.RxFile;
import com.artipie.docker.BlobStore;
import com.artipie.docker.Digest;
import com.artipie.docker.ref.BlobRef;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.cactoos.io.BytesOf;
import org.cactoos.text.HexOf;

/**
 * Asto {@link BlobStore} implementation.
 * @since 0.1
 * @todo #41:30min Refactor this class, make it more readable.
 *  Put method is overcomplicated right now, try to decompose it,
 *  move some logic into new classes or methods.
 * @checkstyle ReturnCountCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class AstoBlobs implements BlobStore {

    /**
     * Vert.x file system used for temporary files.
     */
    private static final FileSystem FILE_SYSTEM = Vertx.vertx().fileSystem();

    /**
     * Storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Storage
     */
    public AstoBlobs(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public CompletableFuture<Optional<Content>> blob(final Digest digest) {
        final Key key = new Key.From(RegistryRoot.V2, new BlobRef(digest).string(), "data");
        return this.asto.exists(key).thenCompose(
            exists -> {
                final CompletionStage<Optional<Content>> stage;
                if (exists) {
                    stage = this.asto.value(key).thenApply(Optional::of);
                } else {
                    stage = CompletableFuture.completedStage(Optional.empty());
                }
                return stage;
            }
        );
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletableFuture<Digest> put(final Content blob) {
        final MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException err) {
            throw new IllegalStateException("This runtime doesn't have SHA-256 algorithm", err);
        }
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
                buf -> Completable.mergeArray(
                    Completable.fromAction(
                        () -> {
                            buf.mark();
                            sha.update(buf);
                            buf.reset();
                        }
                    ),
                    Completable.fromAction(
                        () -> {
                            while (buf.hasRemaining()) {
                                out.write(buf);
                            }
                        }
                    )
                )
            ).andThen(Single.fromCallable(() -> new HexOf(new BytesOf(sha.digest())).asString()))
            .map(Digest.Sha256::new)
            .cast(Digest.class)
            .doOnTerminate(out::close)
            .flatMap(
                digest -> SingleInterop.fromFuture(
                    this.asto.save(
                        new Key.From(RegistryRoot.V2, new BlobRef(digest).string(), "data"),
                        new Content.From(new RxFile(tmp, AstoBlobs.FILE_SYSTEM).flow())
                    ).thenApply(none -> digest)
                )
            ).doOnTerminate(() -> Files.delete(tmp))
            .to(SingleInterop.get()).toCompletableFuture();
    }
}
