/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import java.util.Collection;

/**
 * Image manifest.
 * See <a href="https://docs.docker.com/engine/reference/commandline/manifest/">docker manifest</a>
 *
 * @since 0.2
 */
public interface Manifest {

    /**
     * Read manifest type.
     *
     * @return Type string.
     */
    String mediaType();

    /**
     * Converts manifest to one of types.
     *
     * @param options Types the manifest may be converted to.
     * @return Converted manifest.
     */
    Manifest convert(Collection<String> options);

    /**
     * Read config digest.
     *
     * @return Config digests.
     */
    Digest config();

    /**
     * Read layer digests.
     *
     * @return Layer digests.
     */
    Collection<Layer> layers();

    /**
     * Manifest digest.
     *
     * @return Digest.
     */
    Digest digest();

    /**
     * Read manifest binary content.
     *
     * @return Manifest binary content.
     */
    Content content();

    /**
     * Manifest size.
     *
     * @return Size of the manifest.
     */
    long size();
}
