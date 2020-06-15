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

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.artipie.asto.Storage;
import com.artipie.asto.s3.S3Storage;
import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.BlobKey;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * Integration test for uploading blob to S3.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class UploadBlobS3ITCase {

    /**
     * Mock S3 server.
     */
    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Docker registry used in tests.
     */
    private Docker docker;

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        final S3AsyncClient client = S3AsyncClient.builder()
            .region(Region.of("us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
            )
            .endpointOverride(
                URI.create(String.format("http://localhost:%d", MOCK.getHttpPort()))
            )
            .build();
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(CreateBucketRequest.builder().bucket(bucket).build()).join();
        this.storage = new S3Storage(client, bucket);
        this.docker = new AstoDocker(this.storage);
        this.slice = new DockerSlice("/base", this.docker);
    }

    @Test
    void shouldUpload() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name)).uploads()
            .start()
            .toCompletableFuture().join();
        upload.append(Flowable.just(ByteBuffer.wrap("data".getBytes())))
            .toCompletableFuture().join();
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7"
        );
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(
                    "PUT",
                    String.format(
                        "/base/v2/%s/blobs/uploads/%s?digest=%s",
                        name,
                        upload.uuid(),
                        digest
                    ),
                    "HTTP/1.1"
                ).toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.CREATED)
        );
        MatcherAssert.assertThat(
            this.storage.exists(new BlobKey(new Digest.FromString(digest))).join(),
            new IsEqual<>(true)
        );
    }
}
