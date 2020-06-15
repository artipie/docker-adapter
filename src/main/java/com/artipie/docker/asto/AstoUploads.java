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

import com.artipie.asto.Storage;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import com.artipie.docker.Uploads;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Asto implementation of {@link Uploads}.
 *
 * @since 0.3
 * @todo #169:30min Add unit tests for `AstoUpload` class
 *  `AstoUpload` lacks test coverage. It would be nice to test both `start` and `get` methods
 *  of this class.
 */
public final class AstoUploads implements Uploads {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Ctor.
     *
     * @param asto Asto storage
     * @param name Repository name
     */
    public AstoUploads(final Storage asto, final RepoName name) {
        this.asto = asto;
        this.name = name;
    }

    @Override
    public CompletionStage<Upload> start() {
        final String uuid = UUID.randomUUID().toString();
        final AstoUpload upload = new AstoUpload(this.asto, this.name, uuid);
        return upload.start().thenApply(ignored -> upload);
    }

    @Override
    public CompletionStage<Optional<Upload>> get(final String uuid) {
        return this.asto.list(new UploadKey(this.name, uuid)).thenApply(
            list -> {
                final Optional<Upload> upload;
                if (list.isEmpty()) {
                    upload = Optional.empty();
                } else {
                    upload = Optional.of(new AstoUpload(this.asto, this.name, uuid));
                }
                return upload;
            }
        );
    }
}
