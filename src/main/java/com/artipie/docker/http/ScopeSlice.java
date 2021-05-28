/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.Slice;

/**
 * Slice requiring authorization specified by {@link Scope}.
 *
 * @since 0.11
 */
public interface ScopeSlice extends Slice {

    /**
     * Evaluate authentication scope from HTTP request line.
     *
     * @param line HTTP request line.
     * @return Scope.
     */
    Scope scope(String line);

}
