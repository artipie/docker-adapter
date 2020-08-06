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
package com.artipie.docker.error;

import java.util.Optional;

/**
 * If a blob upload has been cancelled or was never started, this error code may be returned.
 *
 * @since 0.5
 */
public final class UploadUnknownError implements DockerError {

    /**
     * Upload UUID.
     */
    private final String uuid;

    /**
     * Ctor.
     *
     * @param uuid Upload UUID.
     */
    public UploadUnknownError(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String code() {
        return "BLOB_UPLOAD_UNKNOWN";
    }

    @Override
    public String message() {
        return "blob upload unknown to registry";
    }

    @Override
    public Optional<String> detail() {
        return Optional.of(this.uuid);
    }
}
