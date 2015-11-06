package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

public class IdentityManager extends AbstractNoOpResourceManager {

    private Map<String, IdentityProvider> identityProviders;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{Identity.class};
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {

        if (criteria.get("id") != null) {
            return Collections.singletonList(projectMemberToIdentity((String) criteria.get("id")));
        }
        if (criteria.containsKey("name")) {
            Condition search = ((List<Condition>) criteria.get("name")).get(0);
            return searchIdentites((String)search.getValue(), true);
        }
        if (criteria.containsKey("all")) {
            Condition search = ((List<Condition>) criteria.get("all")).get(0);
            return searchIdentites((String)search.getValue(), false);
        }
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        return refreshIdentities(policy.getIdentities());
    }

    /**
     * This method is used to update the cached identities from the policy, which are generated
     * from the users current token, which is only required to be updated once every
     * 16 hours.
     *
     * @param identities Set of identities from Policy.
     * @return Returns a new set of identities after using the api to call out to external
     * sources and update to the newest information. This request is expensive as it will
     * make N requests one for each Passed in identity.
     */
    private List<Identity> refreshIdentities(Set<Identity> identities) {
        List<Identity> identitiesToReturn = new ArrayList<>();
        for (Identity identity : identities) {
            Identity newIdentity = projectMemberToIdentity(identity.getId());
            authorize(newIdentity);
            identitiesToReturn.add(newIdentity);
        }
        return identitiesToReturn;
    }

    public Identity projectMemberToIdentity(String id) {
        String[] split = id.split(":", 2);
        if (split.length != 2) {
            return null;
        }
        Identity identity = null;
        for (IdentityProvider identityProvider : identityProviders.values()) {
            if (identityProvider.scopes().contains(split[0]) && identityProvider.isConfigured()) {
                identity = identityProvider.getIdentity(split[1], split[0]);
                break;
            }
        }
        return identity;
    }

    private List<Identity> searchIdentites(String name, boolean exactMatch) {
        Set<Identity> identities = new HashSet<>();
        for (IdentityProvider identityProvider : identityProviders.values()) {
            if (identityProvider.isConfigured()) {
                identities.addAll(identityProvider.searchIdentities(name, exactMatch));
            }
        }
        return new ArrayList<>(identities);
    }

    public Identity projectMemberToIdentity(ProjectMember member){
        if (member == null){
            return null;
        }
        Identity gotIdentity;
        gotIdentity = projectMemberToIdentity(member.getExternalIdType() + ':' + member.getExternalId());
        if (gotIdentity == null){
            gotIdentity = new Identity(member.getExternalIdType(), member.getExternalId(), member.getName(),
                    null, null, '(' + member.getExternalIdType().split("_")[1].toUpperCase() +  "  not found) " + member.getName());
        }
        return untransform(new Identity(gotIdentity,
                member.getRole(), String.valueOf(member.getProjectId())), false);
    }

    public Identity untransform(Identity identity, boolean error) {
        Identity newIdentity = null;
        for (IdentityProvider identityProvider : identityProviders.values()) {
            if (identityProvider.scopes().contains(identity.getExternalIdType()) && identityProvider.isConfigured()) {
                newIdentity = identityProvider.untransform(identity);
                break;
            }
        }
        if (error && newIdentity == null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, IdentityConstants.INVALID_TYPE, "Identity externalIdType is invalid", null);
        } else if (newIdentity == null){
            return identity;
        } else {
            return newIdentity;
        }
    }

    @Inject
    public void setIdentityProviders(Map<String, IdentityProvider> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public Identity projectMemberToIdentity(Identity identity) {
        Identity newIdentity = null;
        for (IdentityProvider identityProvider : identityProviders.values()) {
            if (identityProvider.scopes().contains(identity.getExternalIdType()) && identityProvider.isConfigured()) {
                newIdentity = identityProvider.transform(identity);
                break;
            }
        }
        if (newIdentity == null){
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, IdentityConstants.INVALID_TYPE, "Identity externalIdType is invalid", null);
        }
        return newIdentity;
    }
}
