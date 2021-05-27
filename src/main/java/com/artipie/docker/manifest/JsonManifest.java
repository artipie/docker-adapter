/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.error.InvalidManifestException;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Image manifest in JSON format.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class JsonManifest implements Manifest {

    /**
     * Manifest digest.
     */
    private final Digest dgst;

    /**
     * JSON bytes.
     */
    private final byte[] source;

    /**
     * Ctor.
     *
     * @param dgst Manifest digest.
     * @param source JSON bytes.
     */
    public JsonManifest(final Digest dgst, final byte[] source) {
        this.dgst = dgst;
        this.source = Arrays.copyOf(source, source.length);
    }

    @Override
    public String mediaType() {
        return Optional.ofNullable(this.json().getString("mediaType", null))
            .orElseThrow(
                () -> new InvalidManifestException("Required field `mediaType` is absent")
            );
    }

    @Override
    public Manifest convert(final Collection<String> options) {
        if (!options.contains("*/*")) {
            final String type = this.mediaType();
            if (!options.contains(type)) {
                throw new IllegalArgumentException(
                    String.format("Cannot convert from '%s' to any of '%s'", type, options)
                );
            }
        }
        return this;
    }

    @Override
    public Digest config() {
        return new Digest.FromString(this.json().getJsonObject("config").getString("digest"));
    }

    @Override
    public Collection<Layer> layers() {
        return Optional.ofNullable(this.json().getJsonArray("layers")).orElseThrow(
            () -> new InvalidManifestException("Required field `layers` is absent")
        ).getValuesAs(JsonValue::asJsonObject).stream()
            .map(JsonLayer::new)
            .collect(Collectors.toList());
    }

    @Override
    public Digest digest() {
        return this.dgst;
    }

    @Override
    public Content content() {
        return new Content.From(this.source);
    }

    @Override
    public long size() {
        return this.source.length;
    }

    /**
     * Read manifest content as JSON object.
     *
     * @return JSON object.
     */
    private JsonObject json() {
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(this.source))) {
            return reader.readObject();
        }
    }

    /**
     * Image layer description in JSON format.
     *
     * @since 0.2
     */
    private static final class JsonLayer implements Layer {

        /**
         * JSON object.
         */
        private final JsonObject json;

        /**
         * Ctor.
         *
         * @param json JSON object.
         */
        private JsonLayer(final JsonObject json) {
            this.json = json;
        }

        @Override
        public Digest digest() {
            return new Digest.FromString(this.json.getString("digest"));
        }

        @Override
        public Collection<URL> urls() {
            return Optional.ofNullable(this.json.getJsonArray("urls")).map(
                urls -> urls.getValuesAs(JsonString.class).stream()
                    .map(
                        str -> {
                            try {
                                return new URL(str.getString());
                            } catch (final MalformedURLException ex) {
                                throw new IllegalArgumentException(ex);
                            }
                        }
                    )
                    .collect(Collectors.toList())
            ).orElseGet(Collections::emptyList);
        }
    }
}
