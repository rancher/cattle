package io.cattle.platform.iaas.api.cluster;

import static io.cattle.platform.core.model.Tables.*;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.ClusterHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ClusterAddHostActionHandler implements ActionHandler {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ClusterHostMapDao clusterHostMapDao;

    @Override
    public String getName() {
        return "host.addhost";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!objectManager.isKind(obj, ClusterConstants.KIND)) {
            return null;
        }
        Host cluster = (Host) obj;

        Long hostToAddId = DataAccessor.fromMap(request.getRequestObject()).withKey(ClusterConstants.ADD_REMOVE_HOST_PARAM).as(Long.class);
        Host hostToAdd = objectManager.loadResource(Host.class, hostToAddId);

        // I'd prefer not to have this check and rely on the DB unique
        // constraint below if possible
        ClusterHostMap existingMapping = clusterHostMapDao.getClusterHostMap(cluster, hostToAdd);
        if (existingMapping != null) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, ClusterConstants.ADD_REMOVE_HOST_PARAM);
        }

        Map<Object, Object> data = new HashMap<>();
        data.put(CLUSTER_HOST_MAP.CLUSTER_ID, cluster.getId());
        data.put(CLUSTER_HOST_MAP.HOST_ID, hostToAdd.getId());
        resourceDao.createAndSchedule(ClusterHostMap.class, objectManager.convertToPropertiesFor(ClusterHostMap.class, data));

        return cluster;
    }

}
