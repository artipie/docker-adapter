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

package com.artipie.docker.ref;

import com.artipie.docker.Digest;
import com.artipie.docker.Tag;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test case for {@link ManifestRef}.
 * @since 0.1
 */
public final class ManifestRefTest {

    @Test
    void resolvesDigestString() {
        MatcherAssert.assertThat(
            new ManifestRef.FromString("sha256:1234").string(),
            Matchers.equalTo("revisions/sha256/1234/link")
        );
    }

    @Test
    void resolvesTagString() {
        MatcherAssert.assertThat(
            new ManifestRef.FromString("1.0").string(),
            Matchers.equalTo("tags/1.0/current/link")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "a:b:c",
        ".123"
    })
    void failsToResolveInvalid(final String string) {
        final Throwable throwable = Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ManifestRef.FromString(string).string()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains(true, "Unsupported reference"),
                    new StringContains(false, string)
                )
            )
        );
    }

    @Test
    void resolvesDigestLink() {
        MatcherAssert.assertThat(
            new ManifestRef.FromDigest(new Digest.Sha256("0000")).string(),
            Matchers.equalTo("revisions/sha256/0000/link")
        );
    }

    @Test
    void resolvesTagLink() {
        MatcherAssert.assertThat(
            new ManifestRef.FromTag(new Tag.Valid("latest")).string(),
            Matchers.equalTo("tags/latest/current/link")
        );
    }
}
