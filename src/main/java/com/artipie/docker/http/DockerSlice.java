/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Docker;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;

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
        this(docker, Permissions.FREE, AuthScheme.NONE);
    }

    /**
     * Ctor.
     *
     * @param docker Docker repository.
     * @param perms Access permissions.
     * @param auth Authentication mechanism used in BasicAuthScheme.
     * @deprecated Use constructor accepting {@link AuthScheme}.
     */
    @Deprecated
    public DockerSlice(final Docker docker, final Permissions perms, final Authentication auth) {
        this(docker, perms, new BasicAuthScheme(auth));
    }

    /**
     * Ctor.
     *
     * @param docker Docker repository.
     * @param perms Access permissions.
     * @param auth Authentication scheme.
     */
    public DockerSlice(final Docker docker, final Permissions perms, final AuthScheme auth) {
        super(
            new ErrorHandlingSlice(
                new SliceRoute(
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(BaseEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new BaseEntity(), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            new ByMethodsRule(RqMethod.HEAD)
                        ),
                        auth(new ManifestEntity.Head(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new ManifestEntity.Get(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            ByMethodsRule.Standard.PUT
                        ),
                        new ManifestEntity.PutAuth(
                            docker, new ManifestEntity.Put(docker), auth, perms
                        )
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(TagsEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new TagsEntity.Get(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(BlobEntity.PATH),
                            new ByMethodsRule(RqMethod.HEAD)
                        ),
                        auth(new BlobEntity.Head(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(BlobEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new BlobEntity.Get(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadEntity.PATH),
                            ByMethodsRule.Standard.POST
                        ),
                        auth(new UploadEntity.Post(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadEntity.PATH),
                            new ByMethodsRule(RqMethod.PATCH)
                        ),
                        auth(new UploadEntity.Patch(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadEntity.PATH),
                            ByMethodsRule.Standard.PUT
                        ),
                        auth(new UploadEntity.Put(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new UploadEntity.Get(docker), perms, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(CatalogEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new CatalogEntity.Get(docker), perms, auth)
                    )
                )
            )
        );
    }

    /**
     * Requires authentication and authorization for slice.
     *
     * @param origin Origin slice.
     * @param perms Access permissions.
     * @param auth Authentication scheme.
     * @return Authorized slice.
     */
    private static Slice auth(
        final ScopeSlice origin,
        final Permissions perms,
        final AuthScheme auth
    ) {
        return new DockerAuthSlice(new AuthScopeSlice(origin, auth, perms));
    }
}
