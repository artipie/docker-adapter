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
package com.artipie.docker.misc;

import com.artipie.http.rq.RqHeaders;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Accept request HTTP header values. For more details check
 * <a href="The Accept request HTTP header">documentation</a>.
 * @since 0.15
 */
public final class AcceptHeader {

    /**
     * Headers.
     */
    private final Iterable<Map.Entry<String, String>> headers;

    /**
     * Ctor.
     * @param headers Headers
     */
    public AcceptHeader(final Iterable<Map.Entry<String, String>> headers) {
        this.headers = headers;
    }

    /**
     * Reads Accept header values.
     * @return Values of the header
     */
    public Set<String> values() {
        return new RqHeaders(this.headers, "Accept").stream().flatMap(
            val -> Arrays.stream(val.split(", "))
        ).map(
            item -> {
                final int index = item.indexOf(";");
                String res = item;
                if (index > 0) {
                    res = res.substring(0, index);
                }
                return res;
            }
        ).collect(Collectors.toSet());
    }
}
