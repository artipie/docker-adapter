/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.RepoName;

/**
 * Operation scope described in Docker Registry auth specification.
 * Scope is an authentication scope for performing an action on resource.
 * See <a href="https://docs.docker.com/registry/spec/auth/scope/">Token Scope Documentation</a>.
 *
 * @since 0.10
 */
public interface Scope {

    /**
     * Get resource type.
     *
     * @return Resource type.
     */
    String type();

    /**
     * Get resource name.
     *
     * @return Resource name.
     */
    String name();

    /**
     * Get resource action.
     *
     * @return Resource action.
     */
    String action();

    /**
     * Get scope as string in default format. See
     * <a href="https://docs.docker.com/registry/spec/auth/scope/">Token Scope Documentation</a>.
     *
     * @return Scope string.
     */
    default String string() {
        return String.format("%s:%s:%s", this.type(), this.name(), this.action());
    }

    /**
     * Abstract decorator for scope.
     *
     * @since 0.10
     */
    abstract class Wrap implements Scope {

        /**
         * Origin scope.
         */
        private final Scope scope;

        /**
         * Ctor.
         *
         * @param scope Origin scope.
         */
        public Wrap(final Scope scope) {
            this.scope = scope;
        }

        @Override
        public final String type() {
            return this.scope.type();
        }

        @Override
        public final String name() {
            return this.scope.name();
        }

        @Override
        public final String action() {
            return this.scope.action();
        }
    }

    /**
     * Scope created from string.
     *
     * @since 0.10
     */
    final class FromString implements Scope {

        /**
         * Source string.
         */
        private final String source;

        /**
         * Ctor.
         *
         * @param source Source string.
         */
        public FromString(final String source) {
            this.source = source;
        }

        @Override
        public String type() {
            return this.part(0);
        }

        @Override
        public String name() {
            return this.part(1);
        }

        @Override
        public String action() {
            return this.part(2);
        }

        /**
         * Extract part with specified index from source string.
         *
         * @param index Zero based index.
         * @return Part string.
         */
        private String part(final int index) {
            final String[] tokens = this.source.split(":");
            if (tokens.length <= index) {
                throw new IllegalStateException(
                    String.format("Source does not have part %d: %s", index, this.source)
                );
            }
            return tokens[index];
        }
    }

    /**
     * Scope for action on repository type resource.
     *
     * @since 0.10
     */
    final class Repository implements Scope {

        /**
         * Resource name.
         */
        private final RepoName name;

        /**
         * Resource action.
         */
        private final String action;

        /**
         * Ctor.
         *
         * @param name Resource name.
         * @param action Resource action.
         */
        public Repository(final RepoName name, final String action) {
            this.name = name;
            this.action = action;
        }

        @Override
        public String type() {
            return "repository";
        }

        @Override
        public String name() {
            return this.name.value();
        }

        @Override
        public String action() {
            return this.action;
        }

        /**
         * Scope for pull action on repository resource.
         *
         * @since 0.10
         */
        static final class Pull extends Scope.Wrap {

            /**
             * Ctor.
             *
             * @param name Resource name.
             */
            Pull(final RepoName name) {
                super(new Repository(name, "pull"));
            }
        }

        /**
         * Scope for push action on repository resource.
         *
         * @since 0.10
         */
        static final class Push extends Scope.Wrap {

            /**
             * Ctor.
             *
             * @param name Resource name.
             */
            Push(final RepoName name) {
                super(new Repository(name, "push"));
            }
        }

        /**
         * Scope for push action on repository resource.
         *
         * @since 0.12
         */
        static final class OverwriteTags extends Scope.Wrap {

            /**
             * Ctor.
             *
             * @param name Resource name.
             */
            OverwriteTags(final RepoName name) {
                super(new Repository(name, "overwrite"));
            }
        }
    }

    /**
     * Scope for action on registry type resource, such as reading repositories catalog.
     *
     * @since 0.11
     */
    final class Registry implements Scope {

        /**
         * Resource name.
         */
        private final String name;

        /**
         * Resource action.
         */
        private final String action;

        /**
         * Ctor.
         *
         * @param name Resource name.
         * @param action Resource action.
         */
        public Registry(final String name, final String action) {
            this.name = name;
            this.action = action;
        }

        @Override
        public String type() {
            return "registry";
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public String action() {
            return this.action;
        }
    }
}
