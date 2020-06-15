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

import com.artipie.http.rq.RequestLineFrom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request by RegEx pattern.
 * @since 0.3
 */
public final class RqByRegex {

    /**
     * Request line.
     */
    private final String line;

    /**
     * Pattern.
     */
    private final Pattern regex;

    /**
     * Ctor.
     * @param line Request line
     * @param regex Regex
     */
    public RqByRegex(final String line, final Pattern regex) {
        this.line = line;
        this.regex = regex;
    }

    /**
     * Matches request path by RegEx pattern.
     *
     * @return Path matcher.
     */
    public Matcher path() {
        final String path = new RequestLineFrom(this.line).uri().getPath();
        final Matcher matcher = this.regex.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Unexpected path: %s", path));
        }
        return matcher;
    }
}
