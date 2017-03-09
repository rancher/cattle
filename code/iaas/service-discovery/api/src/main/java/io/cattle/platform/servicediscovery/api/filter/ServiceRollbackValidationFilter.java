package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.ServiceRevisionTable.*;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ServiceUpgrade;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceRevision;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Named
public class ServiceRollbackValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ServiceDataManager serviceDataMgr;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "service", "dnsService", "externalService" };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_ROLLBACK)) {
            Service service = objectManager.loadResource(Service.class, request.getId());
            ServiceUpgrade upgrade = DataAccessor.field(service, ServiceConstants.FIELD_UPGRADE,
                    jsonMapper,
                    ServiceUpgrade.class);

            if (upgrade != null && upgrade.getInServiceStrategy() != null) {
                InServiceUpgradeStrategy strategy = upgrade.getInServiceStrategy();
                if (strategy.getPreviousLaunchConfig() != null || strategy.getPreviousSecondaryLaunchConfigs() != null) {
                    ServiceUtil.upgradeServiceConfigs(service, strategy, true);
                }
                objectManager.persist(service);
            } else if (service.getPreviousRevisionId() != null) {
                final io.cattle.platform.core.addon.ServiceRollback rollback = jsonMapper.convertValue(
                        request.getRequestObject(),
                        io.cattle.platform.core.addon.ServiceRollback.class);
                Pair<ServiceRevision, ServiceRevision> currentPreviousRevision = null;
                if (rollback != null && !StringUtils.isEmpty(rollback.getRevisionId())) {
                    ServiceRevision currentRevision = serviceDataMgr.getCurrentRevision(service);
                    ServiceRevision previousRevision = objectManager.findAny(ServiceRevision.class,
                            SERVICE_REVISION.ID,
                            rollback.getRevisionId());
                    currentPreviousRevision = Pair.of(currentRevision, previousRevision);
                } else {
                    currentPreviousRevision = serviceDataMgr
                            .getCurrentAndPreviousRevisions(service);
                }
                if (currentPreviousRevision == null || currentPreviousRevision.getRight() == null) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MISSING_REQUIRED,
                            "Failed to find revision to rollback to");
                }
                ServiceRevision previous = currentPreviousRevision.getRight();
                ServiceRevision current = currentPreviousRevision.getLeft();
                Pair<Map<String, Object>, List<Map<String, Object>>> primarySecondaryConfigs = serviceDataMgr
                        .getPrimaryAndSecondaryConfigFromRevision(previous, service);

                Map<String, Object> data = new HashMap<>();
                data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, primarySecondaryConfigs.getLeft());
                data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, primarySecondaryConfigs.getRight());
                data.put(InstanceConstants.FIELD_REVISION_ID, previous.getId());
                data.put(InstanceConstants.FIELD_PREVIOUS_REVISION_ID, current.getId());
                objectManager.setFields(service, data);
            } else if (upgrade == null){
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MISSING_REQUIRED,
                        "Failed to find revision to rollback to");
            }
        }

        return super.resourceAction(type, request, next);
    }
}