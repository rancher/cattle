package io.cattle.platform.api.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

public class ContainerCreateValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;

    public ContainerCreateValidationFilter(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        validateDeploymentUnit(request);

        return super.create(type, request, next);
    }

    @SuppressWarnings("unchecked")
    public void validateDeploymentUnit(ApiRequest request) {
        List<Object> deps = new ArrayList<>();
        Long networkContainerId = DataUtils.getFieldFromRequest(request, "networkContainerId",
                Long.class);
        if (networkContainerId != null) {
            deps.add(networkContainerId);
        }
        List<Long> dataVolumesFrom = DataUtils.getFieldFromRequest(request, "dataVolumesFrom",
                List.class);
        if (dataVolumesFrom != null) {
            deps.addAll(dataVolumesFrom);
        }

        Long sidekickTo = DataUtils.getFieldFromRequest(request, InstanceConstants.FIELD_SIDEKICK_TO,
                Long.class);
        if (sidekickTo != null) {
            deps.add(sidekickTo);
        }
        List<Instance> instances = objectManager.find(Instance.class, INSTANCE.ID, new Condition(ConditionType.IN, deps));
        Long duId = null;
        for (Instance instance : instances) {
            if (instance.getDeploymentUnitId() == null) {
                continue;
            }
            if (duId != null && !duId.equals(instance.getDeploymentUnitId())) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                        "Instances referenced by networkFrom/dataVolumesFrom, belong to different deployment units",
                        null);
            }
            duId = instance.getDeploymentUnitId();
        }
    }
}
