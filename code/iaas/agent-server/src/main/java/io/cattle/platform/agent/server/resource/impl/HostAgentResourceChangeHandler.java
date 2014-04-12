package io.cattle.platform.agent.server.resource.impl;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HostAgentResourceChangeHandler extends GenericTypeAgentResourceChangeHandler {

    private static final String FIELD_PHYSICAL_HOST_UUID = "physicalHostUuid";

    @Override
    protected Field loadField(Object resource, String field) {
        if ( field.equals(FIELD_PHYSICAL_HOST_UUID) && resource instanceof Host ) {
            Long physicalHostId = ((Host)resource).getPhysicalHostId();
            PhysicalHost host = getObjectManager().loadResource(PhysicalHost.class, physicalHostId);

            return new Field(FIELD_PHYSICAL_HOST_UUID, host == null ? null : host.getUuid());
        }

        return super.loadField(resource, field);
    }

    @Override
    protected Field translateField(Map<String, Object> resource, String field, Object value)
            throws MissingDependencyException {
        if ( field.equals(FIELD_PHYSICAL_HOST_UUID) && value != null ) {
            PhysicalHost host = getObjectManager().findOne(PhysicalHost.class, ObjectMetaDataManager.UUID_FIELD, value.toString());
            if ( host == null ) {
                throw new MissingDependencyException();
            }

            return new Field(HostConstants.FIELD_PHYSICAL_HOST_ID, host.getId());
        } else if ( field.equals(HostConstants.FIELD_PHYSICAL_HOST_ID) && value != null ) {
            PhysicalHost host = getObjectManager().loadResource(PhysicalHost.class, value.toString());
            if ( host == null ) {
                throw new MissingDependencyException();
            }

            return new Field(FIELD_PHYSICAL_HOST_UUID, host.getUuid());
        }

        return super.translateField(resource, field, value);
    }

    @Override
    protected Set<String> getAdditionalKeys() {
        return new HashSet<String>(Arrays.asList(FIELD_PHYSICAL_HOST_UUID));
    }

    @Override
    protected Set<String> getChangableKeys() {
        return getAdditionalKeys();
    }

}
