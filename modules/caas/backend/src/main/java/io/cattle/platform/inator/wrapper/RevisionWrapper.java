package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.RevisionConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.inator.factory.InatorServices;
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
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

public class RevisionWrapper {

    RevisionWrapper currentRevision;
    Revision revision;
    StackWrapper stack;
    Map<String, LaunchConfig> lcs = new HashMap<>();
    Map<String, VolumeTemplate> vts = new HashMap<>();
    InatorServices svc;

    public RevisionWrapper(StackWrapper stack, Revision revision, RevisionWrapper currentRevision, InatorServices svc) {
        this.revision = revision;
        this.svc = svc;
        this.stack = stack;
        this.currentRevision = currentRevision;
        init();
    }

    protected void init() {
        vts = getVolumeTemplates();
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
        Object val = getConfig().get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        return CollectionUtils.toMap(val);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> secondaryLaunchConfigs() {
        Object val = getConfig().get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (val == null) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) CollectionUtils.toList(val);
    }

    private void addLaunchConfig(String name, Map<String, Object> lc) {
        InstanceHealthCheck ihc = DataAccessor.fromMap(lc)
                .withKey(InstanceConstants.FIELD_HEALTH_CHECK)
                .as(InstanceHealthCheck.class);

        Object image = lc.get(InstanceConstants.FIELD_IMAGE_UUID);
        if (image == null || StringUtils.isBlank(image.toString()) ||
                image.toString().contains(ServiceConstants.IMAGE_NONE) ||
                image.toString().contains(ServiceConstants.IMAGE_DNS)) {
            return;
        }

        RevisionWrapper current = currentRevision == null ? this : currentRevision;
        ServiceLaunchConfig serviceLaunchConfig = new ServiceLaunchConfig(name, lc, ihc, current, vts, revision.getServiceId() != null, svc);
        lcs.put(name, serviceLaunchConfig);
    }

    public Map<String, LaunchConfig> getLaunchConfigs() {
        return lcs;
    }

    public LaunchConfig getLaunchConfig(String name) {
        if (name != null && name.equals(getName())) {
            name = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }
        return lcs.get(name);
    }

    protected Map<String, Object> getConfig() {
        return DataAccessor.fieldMap(revision, RevisionConstants.FIELD_CONFIG);
    }

    public boolean isStartFirst() {
        return DataAccessor.fromMap(getConfig())
                .withDefault(false)
                .withKey(ServiceConstants.FIELD_START_FIRST_ON_UPGRADE)
                .as(Boolean.class);
    }

    protected Map<String, VolumeTemplate> getVolumeTemplates() {
        if (stack == null) {
            return Collections.emptyMap();
        }

        return svc.serviceDao.getVolumeTemplates(stack.getId()).stream()
            .collect(Collectors.toMap(
                    (vt) -> vt.getName(),
                    (x) -> x));
    }

    public Long getServiceId() {
        return revision.getServiceId();
    }

    public String getName() {
        Map<String, Object> config = DataAccessor.fieldMap(revision, RevisionConstants.FIELD_CONFIG);
        return ObjectUtils.toString(config.get(ObjectMetaDataManager.NAME_FIELD));
    }

    public Long getId() {
        return revision.getId();
    }

    public long getAccountId() {
        return revision.getAccountId();
    }

}
