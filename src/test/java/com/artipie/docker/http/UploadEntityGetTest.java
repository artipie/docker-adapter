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
package com.artipie.docker.http;

import com.artipie.asto.fs.FileStorage;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link DockerSlice}.
 * Upload GET endpoint.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidDuplicateLiterals"})
public final class UploadEntityGetTest {
    /**
     * Vertx instance.
     */
    private Vertx vertx;

    /**
     * Docker registry used in tests.
     */
    private Docker docker;

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp(@TempDir final Path tmp) {
        this.vertx = Vertx.vertx();
        this.docker = new AstoDocker(new FileStorage(tmp, this.vertx.fileSystem()));
        this.slice = new DockerSlice("/base", this.docker);
    }

    @AfterEach
    void tearDown() {
        this.vertx.close();
    }

    @Test
    void shouldReturnZeroOffsetAfterUploadStarted() {
        final String name = "test";
        final String uuid = this.startUpload(name);
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, uuid);
        final Response response = this.slice.response(
            new RequestLine("GET", String.format("/base%s", path), "HTTP/1.1").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.NO_CONTENT),
                    new RsHasHeaders(
                        new Header("Range", "0-0"),
                        new Header("Content-Length", "0"),
                        new Header("Docker-Upload-UUID", uuid)
                    )
                )
            )
        );
    }

    @Test
    void shouldReturnZeroOffsetAfterOneByteUploaded() throws InterruptedException {
        final String name = "test";
        final String uuid = this.startUpload(name);
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, uuid);
        final CountDownLatch uploaded = new CountDownLatch(1);
        final CountDownLatch requested = new CountDownLatch(1);
        final Thread thread = new Thread(
            () -> this.slice.response(
                new RequestLine("PATCH", String.format("/base%s", path), "HTTP/1.1").toString(),
                Collections.emptyList(),
                subscriber -> {
                    try {
                        subscriber.onNext(ByteBuffer.allocate(1));
                        uploaded.countDown();
                        requested.await();
                        subscriber.onComplete();
                    } catch (final InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            )
        );
        thread.start();
        uploaded.await();
        final Response response = this.slice.response(
            new RequestLine("GET", String.format("/base%s", path), "HTTP/1.1").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.NO_CONTENT),
                    new RsHasHeaders(
                        new Header("Range", "0-0"),
                        new Header("Content-Length", "0"),
                        new Header("Docker-Upload-UUID", uuid)
                    )
                )
            )
        );
        requested.countDown();
        thread.join();
    }

    @Test
    void shouldReturnOffsetDuringUpload() throws InterruptedException, ExecutionException {
        final String name = "test";
        final String uuid = this.startUpload(name);
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, uuid);
        final CountDownLatch uploaded = new CountDownLatch(1);
        final CountDownLatch requested = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<Response> patch = executor.submit(
            () -> this.slice.response(
                new RequestLine("PATCH", String.format("/base%s", path), "HTTP/1.1").toString(),
                Collections.emptyList(),
                subscriber -> {
                    try {
                        //@checkstyle MagicNumberCheck (5 lines)
                        subscriber.onNext(ByteBuffer.allocate(1024));
                        uploaded.countDown();
                        requested.await();
                        subscriber.onNext(ByteBuffer.allocate(1024));
                        subscriber.onComplete();
                    } catch (final InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            )
        );
        uploaded.await();
        final Response get = this.slice.response(
            new RequestLine("GET", String.format("/base%s", path), "HTTP/1.1").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            get,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.NO_CONTENT),
                    new RsHasHeaders(
                        new Header("Range", "0-1023"),
                        new Header("Content-Length", "0"),
                        new Header("Docker-Upload-UUID", uuid)
                    )
                )
            )
        );
        requested.countDown();
        MatcherAssert.assertThat(
            patch.get(),
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.NO_CONTENT),
                    new RsHasHeaders(
                        new Header("Range", "0-2047"),
                        new Header("Content-Length", "0"),
                        new Header("Docker-Upload-UUID", uuid),
                        new Header("Location", path)
                    )
                )
            )
        );
        executor.shutdown();
    }

    @Test
    void shouldReturnNotFoundWhenUploadNotExists() {
        final Response response = this.slice.response(
            new RequestLine("GET", "/base/v2/test/blobs/uploads/12345", "HTTP/1.1").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    private String startUpload(final String name) {
        final Upload upload = this.docker.repo(new RepoName.Valid(name))
            .startUpload()
            .toCompletableFuture().join();
        return upload.uuid();
    }
}
