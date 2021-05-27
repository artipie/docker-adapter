/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Uploads;

/**
 * Cache implementation of {@link Repo}.
 *
 * @since 0.3
 */
public final class CacheRepo implements Repo {

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Origin repository.
     */
    private final Repo origin;

    /**
     * Cache repository.
     */
    private final Repo cache;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param origin Origin repository.
     * @param cache Cache repository.
     */
    public CacheRepo(final RepoName name, final Repo origin, final Repo cache) {
        this.name = name;
        this.origin = origin;
        this.cache = cache;
    }

    @Override
    public Layers layers() {
        return new CacheLayers(this.origin.layers(), this.cache.layers());
    }

    @Override
    public Manifests manifests() {
        return new CacheManifests(this.name, this.origin, this.cache);
    }

    @Override
    public Uploads uploads() {
        throw new UnsupportedOperationException();
    }
}
