/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.http.rq.RqHeaders;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Accept request HTTP header values. For more details check
 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept">documentation</a>.
 * @since 0.15
 * @todo #499:30min Better support of Accept HTTP header: accept header values can have
 *  quality weight and should be ordered according to this weight. Let's support this feature
 *  and move this functionality to http repository. For more information check documentation:
 *  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept
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
