package io.cattle.platform.iaas.api.filter.instance;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ContainerCreateValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objMgr;
    @Inject
    InstanceDao instanceDao;

    @Override
    public String[] getTypes() {
        return new String[] { "container" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        validateDeploymentUnit(request);

        Object object = super.create(type, request, next);
        if (object instanceof Instance) {
            setRevision(request, object);
        } else if (object instanceof List) {
            for (Object i : (List<?>) object) {
                setRevision(request, i);
            }
        }
        return object;
    }

    public void setRevision(ApiRequest request, Object object) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        InstanceRevision revision = instanceDao.createRevision((Instance) object, data, true);
        objMgr.setFields(object, InstanceConstants.FIELD_REVISION_ID, revision.getId());
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
        List<Instance> instances = objMgr.find(Instance.class, INSTANCE.ID, new Condition(ConditionType.IN, deps));
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
