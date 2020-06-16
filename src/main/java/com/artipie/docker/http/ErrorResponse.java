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
package com.artipie.docker.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.json.Json;

/**
 * Docker response in case of errors.
 * @todo #117:15min Finish implementation of test.
 *  In order to ensure that errorr response works correctly, the ErrorResponseTest has been written.
 *  The test is disabled for now, we must fix that

 * @since 0.2
 */
abstract class ErrorResponse extends Response.Wrap {

    /**
     * Ctor.
     *
     * @param status Http status
     * @param buff Origin response
     */
    private ErrorResponse(final RsStatus status, final ByteBuffer buff) {
        super(
            new RsWithBody(
                new RsWithHeaders(
                    new RsWithStatus(status),
                    new Headers.From(
                        new Header("Content-Length", String.valueOf(buff.position())),
                        new Header("Content-Type", "application/json; charset=utf-8")
                    )
                ),
                buff
            )
        );
    }

    /**
     * Ctor.
     *
     * @param status HTTP status of response
     * @param code Code of response
     * @param message Message of response
     * @param detail More detail about of  response
     * @checkstyle LineLengthCheck (10 lines)
     * @checkstyle ParameterNumberCheck (4 lines)
     */
    ErrorResponse(final RsStatus status, final String code, final String message, final String detail) {
        this(
            status,
            ByteBuffer.wrap(
                Json.createObjectBuilder()
                    .add(
                        "errors",
                        Json.createObjectBuilder().add("code", code)
                            .add("message", message)
                            .add("detail", detail)
                    )
                    .build().toString().getBytes(StandardCharsets.UTF_8)
            )
        );
    }
}
