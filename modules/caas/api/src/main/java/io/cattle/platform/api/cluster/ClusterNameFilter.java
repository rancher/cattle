package io.cattle.platform.api.cluster;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import static io.cattle.platform.core.model.Tables.CLUSTER;

public class ClusterNameFilter extends AbstractValidationFilter {

    private static final String CLUSTER_NAME_NOT_UNIQUE = "ClusterNameNotUnique";
    ObjectManager objectManager;

    public ClusterNameFilter(ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Cluster cluster = request.proxyRequestObject(Cluster.class);
        long accountId = ((Policy) ApiContext.getContext().getPolicy()).getAccountId();
        validateClusterName(cluster.getName(), accountId);
        return super.create(type, request, next);
    }

    void validateClusterName(String clusterName, Long accountId) {
        Cluster cluster = objectManager.findOne(Cluster.class, CLUSTER.NAME, clusterName, CLUSTER.REMOVED, null);
        if (cluster != null) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, CLUSTER_NAME_NOT_UNIQUE);
        }
    }
}
