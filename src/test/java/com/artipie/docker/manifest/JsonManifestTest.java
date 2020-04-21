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
package com.artipie.docker.manifest;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JsonManifest}.
 *
 * @since 0.2
 */
class JsonManifestTest {

    @Test
    void shouldReadMediaType() throws Exception {
        final JsonManifest manifest = new JsonManifest(
            new Content.From("{\"mediaType\":\"something\"}".getBytes())
        );
        MatcherAssert.assertThat(
            manifest.mediaType().toCompletableFuture().get(),
            new IsEqual<>("something")
        );
    }

    @Test
    void shouldConvertToSameType() throws Exception {
        final JsonManifest manifest = new JsonManifest(
            new Content.From("{\"mediaType\":\"type2\"}".getBytes())
        );
        MatcherAssert.assertThat(
            manifest.convert(Arrays.asList("type1", "type2")).toCompletableFuture().get(),
            new IsEqual<>(manifest)
        );
    }

    @Test
    void shouldFailConvertToUnknownType() {
        final JsonManifest manifest = new JsonManifest(
            new Content.From("{\"mediaType\":\"typeA\"}".getBytes())
        );
        final ExecutionException exception = Assertions.assertThrows(
            ExecutionException.class,
            () -> manifest.convert(Collections.singleton("typeB")).toCompletableFuture().get()
        );
        MatcherAssert.assertThat(
            exception.getCause(),
            new IsInstanceOf(IllegalArgumentException.class)
        );
    }

    @Test
    void shouldReadContent() {
        final byte[] data = "data".getBytes();
        final JsonManifest manifest = new JsonManifest(new Content.From(data));
        MatcherAssert.assertThat(
            new Remaining(new Concatenation(manifest.content()).single().blockingGet()).bytes(),
            new IsEqual<>(data)
        );
    }
}
