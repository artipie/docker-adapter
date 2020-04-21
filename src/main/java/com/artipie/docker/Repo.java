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

package com.artipie.docker;

import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Docker repository files and metadata.
 * @since 0.1
 */
public interface Repo {

    /**
     * Layer link by algorithm and digest.
     * <p>
     * layerLinkPathSpec:
     * <code>repositories/&lt;name&gt;/_layers/
     * &lt;algorithm&gt;/&lt;hex digest&gt;/link</code>
     * </p>
     * @param alg Digest algorithm
     * @param digest Digest hex string
     * @return Digest of layer blob
     */
    Digest layer(String alg, String digest);

    /**
     * Resolve docker image manifest file by reference.
     * @param ref Manifest reference
     * @return Flow with manifest data, or empty if absent
     */
    CompletionStage<Optional<Manifest>> manifest(ManifestRef ref);
}
