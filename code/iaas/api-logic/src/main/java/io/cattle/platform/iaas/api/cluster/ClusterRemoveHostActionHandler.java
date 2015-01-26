package io.cattle.platform.iaas.api.cluster;

import javax.inject.Inject;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.model.ClusterHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

public class ClusterRemoveHostActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager processManager;

    @Inject
    ClusterHostMapDao clusterHostMapDao;

    @Override
    public String getName() {
        return "host.removehost";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!objectManager.isKind(obj, ClusterConstants.KIND)) {
            return null;
        }
        Host cluster = (Host)obj;

        Long hostToRemoveId = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ClusterConstants.ADD_REMOVE_HOST_PARAM)
                .as(Long.class);
        Host hostToRemove = objectManager.loadResource(Host.class, hostToRemoveId);

        ClusterHostMap mapping = clusterHostMapDao.getClusterHostMap(cluster, hostToRemove);

        if (mapping == null) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, ClusterConstants.ADD_REMOVE_HOST_PARAM);
        }
        processManager.scheduleProcessInstance(ClusterConstants.CLUSTER_HOST_MAP_REMOVE_PROCESS, mapping, null);
        return cluster;
    }

}
