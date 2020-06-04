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

import com.artipie.docker.Docker;
import com.artipie.http.Slice;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.TrimPathSlice;
import java.util.regex.Pattern;

/**
 * Slice implementing Docker Registry HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/">Docker Registry HTTP API V2</a>.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
public final class DockerSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param docker Docker repository.
     */
    public DockerSlice(final Docker docker) {
        this("", docker);
    }

    /**
     * Ctor.
     *
     * @param base Base path. Base path should start with "/", empty string means no base path.
     * @param docker Docker repository.
     */
    public DockerSlice(final String base, final Docker docker) {
        super(
            new TrimPathSlice(
                new SliceRoute(
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(BaseEntity.PATH),
                            new RtRule.ByMethod(RqMethod.GET)
                        ),
                        new BaseEntity()
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            new RtRule.ByMethod(RqMethod.HEAD)
                        ),
                        new ManifestEntity.Head(docker)
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            new RtRule.ByMethod(RqMethod.GET)
                        ),
                        new ManifestEntity.Get(docker)
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            new RtRule.ByMethod(RqMethod.PUT)
                        ),
                        new ManifestEntity.Put(docker)
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(BlobEntity.PATH),
                            new RtRule.ByMethod(RqMethod.HEAD)
                        ),
                        new BlobEntity.Head(docker)
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(BlobEntity.PATH),
                            new RtRule.ByMethod(RqMethod.GET)
                        ),
                        new BlobEntity.Get(docker)
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(UploadEntity.PATH),
                            new RtRule.ByMethod(RqMethod.POST)
                        ),
                        new UploadEntity.Post(docker)
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(UploadEntity.PATH),
                            new RtRule.ByMethod(RqMethod.PATCH)
                        ),
                        new UploadEntity.Patch(docker)
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(UploadEntity.PATH),
                            new RtRule.ByMethod(RqMethod.PUT)
                        ),
                        new UploadEntity.Put(docker)
                    ),
                    new SliceRoute.Path(
                        new RtRule.Multiple(
                            new RtRule.ByPath(UploadEntity.PATH),
                            new RtRule.ByMethod(RqMethod.GET)
                        ),
                        new UploadEntity.Get(docker)
                    )
                ),
                Pattern.compile(String.format("^(?:%s)(\\/.*)?", base))
            )
        );
    }
}
