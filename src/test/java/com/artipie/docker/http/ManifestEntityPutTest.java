/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.TrustedBlobSource;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Manifest PUT endpoint.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ManifestEntityPutTest {

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    /**
     * Docker used in tests.
     */
    private Docker docker;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
        this.slice = new DockerSlice(this.docker);
    }

    @Test
    void shouldPushManifestByTag() {
        final String path = "/v2/my-alpine/manifests/1";
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.PUT, String.format("%s", path)).toString(),
                Headers.EMPTY,
                this.manifest()
            ),
            new ResponseMatcher(
                RsStatus.CREATED,
                new Header("Location", path),
                new Header("Content-Length", "0"),
                new Header(
                    "Docker-Content-Digest",
                    "sha256:ef0ff2adcc3c944a63f7cafb386abc9a1d95528966085685ae9fab2a1c0bedbf"
                )
            )
        );
    }

    @Test
    void shouldPushManifestByDigest() {
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "ef0ff2adcc3c944a63f7cafb386abc9a1d95528966085685ae9fab2a1c0bedbf"
        );
        final String path = String.format("/v2/my-alpine/manifests/%s", digest);
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.PUT, String.format("%s", path)).toString(),
                Headers.EMPTY,
                this.manifest()
            ),
            new ResponseMatcher(
                RsStatus.CREATED,
                new Header("Location", path),
                new Header("Content-Length", "0"),
                new Header("Docker-Content-Digest", digest)
            )
        );
    }

    /**
     * Create manifest content.
     *
     * @return Manifest content.
     */
    private Flowable<ByteBuffer> manifest() {
        final byte[] content = "config".getBytes();
        final Blob config = this.docker.repo(new RepoName.Valid("my-alpine")).layers()
            .put(new TrustedBlobSource(content))
            .toCompletableFuture().join();
        final byte[] data = String.format(
            "{\"config\":{\"digest\":\"%s\"},\"layers\":[],\"mediaType\":\"my-type\"}",
            config.digest().string()
        ).getBytes();
        return Flowable.just(ByteBuffer.wrap(data));
    }
}
