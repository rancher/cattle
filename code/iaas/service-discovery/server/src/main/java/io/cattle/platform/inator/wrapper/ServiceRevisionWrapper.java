package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.ServiceRevisionConstants;
import io.cattle.platform.core.model.ServiceRevision;
import io.cattle.platform.inator.factory.Services;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.inator.launchconfig.impl.ServiceLaunchConfig;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRevisionWrapper {

    ServiceRevision serviceRevision;
    Map<String, LaunchConfig> lcs = new HashMap<>();
    Services svc;

    public ServiceRevisionWrapper(ServiceRevision serviceRevision, Services svc) {
        this.serviceRevision = serviceRevision;
        this.svc = svc;
        init();
    }

    protected void init() {
        Map<String, Object> lc = primaryLaunchConfig();
        addLaunchConfig(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME, lc);

        for (Map<?, ?> secondary : secondaryLaunchConfigs()) {
            String name = DataAccessor.fromMap(secondary)
                    .withKey(ObjectMetaDataManager.NAME_FIELD)
                    .withDefault("")
                    .as(String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> temp = (Map<String, Object>) secondary;
            addLaunchConfig(name, temp);
        }
    }

    private Map<String, Object> primaryLaunchConfig() {
        Object val = DataAccessor.fieldMap(serviceRevision, ServiceRevisionConstants.FIELD_CONFIG)
                .get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        return CollectionUtils.toMap(val);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> secondaryLaunchConfigs() {
        Object val = DataAccessor.fieldMap(serviceRevision, ServiceRevisionConstants.FIELD_CONFIG)
                .get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (val == null) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) CollectionUtils.toList(val);
    }

    private void addLaunchConfig(String name, Map<String, Object> lc) {
        InstanceHealthCheck ihc = DataAccessor.fromMap(lc)
                .withKey(InstanceConstants.FIELD_HEALTH_CHECK)
                .as(svc.jsonMapper, InstanceHealthCheck.class);

        ServiceLaunchConfig serviceLaunchConfig = new ServiceLaunchConfig(name, lc, ihc, this, svc);
        lcs.put(name, serviceLaunchConfig);
    }

    public Map<String, LaunchConfig> getLaunchConfigs() {
        return lcs;
    }

    public boolean isStartFirst() {
        return DataAccessor.fieldBool(serviceRevision, ServiceConstants.FIELD_START_FIRST_ON_UPGRADE);
    }

    public boolean isRetainIP() {
        return DataAccessor.fieldBool(serviceRevision, ServiceConstants.FIELD_RETAIN_IP);
    }

    public Long getServiceId() {
        return serviceRevision.getServiceId();
    }

    public String getName() {
        Map<String, Object> config = DataAccessor.fieldMap(serviceRevision, ServiceRevisionConstants.FIELD_CONFIG);
        return ObjectUtils.toString(config.get(ObjectMetaDataManager.NAME_FIELD));
    }

    public Long getId() {
        return serviceRevision.getId();
    }

}
