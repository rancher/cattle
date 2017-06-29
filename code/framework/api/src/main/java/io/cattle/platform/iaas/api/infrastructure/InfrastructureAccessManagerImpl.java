package io.cattle.platform.iaas.api.infrastructure;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.netflix.config.DynamicStringProperty;

public class InfrastructureAccessManagerImpl implements InfrastructureAccessManager {

    private static final DynamicStringProperty MODIFY_INFRA_ROLES = ArchaiusUtil.getString("modify.infrastructure.roles");

    private Set<String> modifyInfraRoles = new HashSet<>();

    public InfrastructureAccessManagerImpl() {
        super();

        String prop = MODIFY_INFRA_ROLES.get();
        Set<String> roles = new HashSet<>(Arrays.asList(prop.split(",")));
        modifyInfraRoles = roles;

        MODIFY_INFRA_ROLES.addCallback(new Runnable() {
            @Override
            public void run() {
                String prop = MODIFY_INFRA_ROLES.get();
                Set<String> roles = new HashSet<>(Arrays.asList(prop.split(",")));
                modifyInfraRoles = roles;
            }
        });
    }

    @Override
    public boolean canModifyInfrastructure(Policy policy) {
        // Return true if modifyInfraRoles contains any of the roles in
        // policy.getRoles
        return modifyInfraRoles.stream().anyMatch(policy.getRoles()::contains);
    }
}
