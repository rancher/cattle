package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.iaas.api.auth.ExternalIdHandler;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;

import javax.inject.Inject;

public class GithubExternalIdHandler implements ExternalIdHandler {


    @Inject
    GithubClient githubClient;

    @Override
    public ExternalId transform(ExternalId externalId) {
        String id;
        String name;
        GithubAccountInfo githubAccountInfo;
        switch (externalId.getType()){
            case GithubUtils.USER_SCOPE:
                githubAccountInfo = githubClient.getUserIdByName(externalId.getId());
                id = githubAccountInfo.getAccountId();
                name = githubAccountInfo.getAccountName();
                return new ExternalId(id, GithubUtils.USER_SCOPE, name);
            case GithubUtils.ORG_SCOPE:
                    githubAccountInfo = githubClient.getOrgIdByName(externalId.getId());
                    id = githubAccountInfo.getAccountId();
                    name = githubAccountInfo.getAccountName();
                return new ExternalId(id, GithubUtils.ORG_SCOPE, name);
            case GithubUtils.TEAM_SCOPE:
                    String org = githubClient.getTeamOrgById(externalId.getId());
                    return new ExternalId(externalId.getId(), GithubUtils.TEAM_SCOPE, org);
            default:
                return null;
        }
    }

    @Override
    public ExternalId untransform(ExternalId externalId) {
        switch (externalId.getType()) {
        case GithubUtils.USER_SCOPE:
            return new ExternalId(externalId.getName(), externalId.getType(), externalId.getName());
        case GithubUtils.ORG_SCOPE:
            return new ExternalId(externalId.getName(), externalId.getType(), externalId.getName());
        case GithubUtils.TEAM_SCOPE:
            return externalId;
        default:
            return null;
    }
}
}
