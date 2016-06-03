package io.cattle.platform.iaas.api.filter.snapshot;

import static io.cattle.platform.core.model.tables.SnapshotTable.*;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class SnapshotValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Snapshot.class };
    }

    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if ("remove".equalsIgnoreCase(request.getAction())) {
            validateSnapshotRemove(request);
        }
        return super.resourceAction(type, request, next);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        validateSnapshotRemove(request);
        return super.delete(type, id, request, next);
    }

    void validateSnapshotRemove(ApiRequest request) {
        Snapshot snapshot = objectManager.loadResource(Snapshot.class, request.getId());
        Map<Object, Object> criteria = new HashMap<Object, Object>();
        criteria.put(SNAPSHOT.VOLUME_ID, snapshot.getVolumeId());
        criteria.put(SNAPSHOT.REMOVED, null);
        criteria.put(SNAPSHOT.ID, new Condition(ConditionType.GT, snapshot.getId()));
        List<Snapshot> snapshots = objectManager.find(Snapshot.class, criteria);
        if (snapshots.size() == 0) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.INVALID_STATE,
                    "This snapshot cannot be removed because it is the latest one for the volume.", null);
        }
    }
}
