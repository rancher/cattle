package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;

import javax.inject.Inject;

public class HostVnetMatchConstraintProvider implements AllocationConstraintsProvider {

    AllocatorDao allocatorDao;
    ObjectManager objectManager;
    JsonMapper jsonMapper;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        for ( Nic nic : attempt.getNics() ) {
            Network network = objectManager.loadResource(Network.class, nic.getNetworkId());
            boolean dynamic = DataAccessor.fields(network)
                                    .withKey(NetworkConstants.FIELD_DYNAMIC_CREATE_VNET)
                                    .withDefault(false)
                                    .as(Boolean.class);

            boolean add = false;
            if ( dynamic ) {
                if ( nic.getVnetId() != null ) {
                    add = true;
                }
            } else {
                add = true;
            }

            if ( add ) {
                constraints.add(new HostVnetMatchConstraint(nic.getId(), objectManager, allocatorDao));
            }
        }
    }

    public AllocatorDao getAllocatorDao() {
        return allocatorDao;
    }

    @Inject
    public void setAllocatorDao(AllocatorDao allocatorDao) {
        this.allocatorDao = allocatorDao;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}
