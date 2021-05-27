/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.RepoName;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Scope}.
 *
 * @since 0.10
 */
class ScopeTest {

    @Test
    void repositoryPullScope() {
        MatcherAssert.assertThat(
            new Scope.Repository.Pull(new RepoName.Valid("samalba/my-app")).string(),
            new IsEqual<>("repository:samalba/my-app:pull")
        );
    }

    @Test
    void repositoryPushScope() {
        MatcherAssert.assertThat(
            new Scope.Repository.Push(new RepoName.Valid("busybox")).string(),
            new IsEqual<>("repository:busybox:push")
        );
    }

    @Test
    void registryScope() {
        MatcherAssert.assertThat(
            new Scope.Registry("catalog", "*").string(),
            new IsEqual<>("registry:catalog:*")
        );
    }

    @Test
    void scopeFromString() {
        final Scope scope = new Scope.FromString("repository:my-alpine:pull");
        MatcherAssert.assertThat(
            "Has expected type",
            scope.type(),
            new IsEqual<>("repository")
        );
        MatcherAssert.assertThat(
            "Has expected name",
            scope.name(),
            new IsEqual<>("my-alpine")
        );
        MatcherAssert.assertThat(
            "Has expected action",
            scope.action(),
            new IsEqual<>("pull")
        );
    }

    @Test
    void scopeFromInvalidString() {
        final Scope scope = new Scope.FromString("something");
        Assertions.assertThrows(
            IllegalStateException.class,
            scope::name,
            "Name cannot be parsed"
        );
        Assertions.assertThrows(
            IllegalStateException.class,
            scope::action,
            "Action cannot be parsed"
        );
    }
}
