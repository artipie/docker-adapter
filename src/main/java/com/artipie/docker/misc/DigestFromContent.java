/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletionStage;
import org.cactoos.io.BytesOf;
import org.cactoos.text.HexOf;

/**
 * Digest from content.
 * @since 0.2
 */
public final class DigestFromContent {

    /**
     * Content.
     */
    private final Content content;

    /**
     * Ctor.
     * @param content Content publisher
     */
    public DigestFromContent(final Content content) {
        this.content = content;
    }

    /**
     * Calculates digest from content.
     * @return CompletionStage from digest
     */
    public CompletionStage<Digest> digest() {
        final MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException err) {
            throw new IllegalStateException("This runtime doesn't have SHA-256 algorithm", err);
        }
        return Flowable.fromPublisher(this.content)
            .flatMapCompletable(
                buf -> Completable.fromAction(
                    () -> {
                        buf.mark();
                        sha.update(buf);
                        buf.reset();
                    }
                )
            )
            .<Digest>andThen(
                Single.fromCallable(
                    () -> new Digest.Sha256(new HexOf(new BytesOf(sha.digest())).asString())
                )
            )
            .to(SingleInterop.get()).toCompletableFuture();
    }

}
