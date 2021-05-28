/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.JoinedCatalogSource;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Cache {@link Docker} implementation.
 *
 * @since 0.3
 */
public final class CacheDocker implements Docker {

    /**
     * Origin repository.
     */
    private final Docker origin;

    /**
     * Cache repository.
     */
    private final Docker cache;

    /**
     * Ctor.
     *
     * @param origin Origin repository.
     * @param cache Cache repository.
     */
    public CacheDocker(final Docker origin, final Docker cache) {
        this.origin = origin;
        this.cache = cache;
    }

    @Override
    public Repo repo(final RepoName name) {
        return new CacheRepo(name, this.origin.repo(name), this.cache.repo(name));
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        return new JoinedCatalogSource(from, limit, this.origin, this.cache).catalog();
    }
}
