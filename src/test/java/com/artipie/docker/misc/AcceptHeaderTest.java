/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AcceptHeader}.
 * @since 0.16
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AcceptHeaderTest {

    @Test
    void parsesMultipleHeaders() {
        MatcherAssert.assertThat(
            new AcceptHeader(
                // @checkstyle LineLengthCheck (5 lines)
                new Headers.From(new Header("Accept", "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8"), new Header("Accept", "abc/123, another"))
            ).values(),
            Matchers.containsInAnyOrder("text/html", "application/xhtml+xml", "application/xml", "image/webp", "*/*", "abc/123", "another")
        );
    }

    @Test
    void parsesSingle() {
        MatcherAssert.assertThat(
            new AcceptHeader(
                new Headers.From(new Header("Accept", "application/json"))
            ).values(),
            Matchers.containsInAnyOrder("application/json")
        );
    }

}
