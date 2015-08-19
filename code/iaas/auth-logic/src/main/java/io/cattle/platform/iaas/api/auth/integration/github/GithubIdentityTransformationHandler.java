package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityTransformationHandler;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Set;
import javax.inject.Inject;

public class GithubIdentityTransformationHandler extends GithubConfigurable implements IdentityTransformationHandler {


    @Inject
    GithubIdentitySearchProvider githubIdentitySearchProvider;
    @Inject
    GithubUtils githubUtils;
    @Inject
    GithubTokenCreator githubTokenCreator;
    @Inject
    GithubClient githubClient;
    @Inject
    AuthTokenDao authTokenDao;

    @Override
    public Identity transform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case GithubConstants.USER_SCOPE:
            case GithubConstants.ORG_SCOPE:
            case GithubConstants.TEAM_SCOPE:
                return githubIdentitySearchProvider.getIdentity(identity.getExternalId(), identity.getExternalIdType());
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, IdentityConstants.INVALID_TYPE,
                        "Github does not provide: " + identity.getExternalIdType(), null );
        }
    }

    @Override
    public Identity untransform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case GithubConstants.USER_SCOPE:
            case GithubConstants.ORG_SCOPE:
            case GithubConstants.TEAM_SCOPE:
                return identity;
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, IdentityConstants.INVALID_TYPE,
                        "Github does not provide: " + identity.getExternalIdType(), null );
        }
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        return githubIdentitySearchProvider.getIdentities(account);
    }

    @Override
    public Set<String> scopes() {
        return GithubConstants.SCOPES;
    }

    @Override
    public String getName() {
        return GithubConstants.TRANSFORMATION_HANDLER;
    }
}
