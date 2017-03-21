package io.cattle.platform.iaas.api.filter.instance;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.addon.MountEntry;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class InstanceOutputFilter extends CachedOutputFilter<Map<Long, Map<String, Object>>> {
    @Inject
    ServiceDao serviceDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    VolumeDao volumeDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {};
    }

    @Override
    public String[] getTypes() {
        return new String[]{InstanceConstants.TYPE_CONTAINER};
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (request == null || !"v1".equals(request.getVersion())) {
            if (original instanceof Instance) {
                Map<Long, Map<String, Object>> data = getCached(request);
                if (data != null) {
                    Map<String, Object> fields = data.get(((Instance) original).getId());
                    if (fields != null) {
                        converted.getFields().putAll(fields);
                    }
                }
            }
        }

        List<Long> networkIds = DataAccessor.fieldLongList(original, InstanceConstants.FIELD_NETWORK_IDS);
        if (networkIds.size() > 0) {
            IdFormatter idF = ApiContext.getContext().getIdFormatter();
            converted.getFields().put(InstanceConstants.FIELD_PRIMARY_NETWORK_ID, idF.formatId(NetworkConstants.KIND_NETWORK, networkIds.get(0)));
        }

        Map<String, Object> labels = CollectionUtils.toMap(converted.getFields().get(InstanceConstants.FIELD_LABELS));
        if ("rancher-agent".equals(labels.get("io.rancher.container.system")) &&
                "rancher-agent".equals(converted.getFields().get(ObjectMetaDataManager.NAME_FIELD))) {
            Map<String, URL> actions = converted.getActions();
            if (actions != null) {
                actions.remove("remove");
                actions.remove("stop");
                actions.remove("start");
                actions.remove("restart");
            }
        }

        return converted;
    }

    @Override
    protected Map<Long, Map<String, Object>> newObject(ApiRequest apiRequest) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        List<Long> ids = getIds(apiRequest);
        IdFormatter idF = ApiContext.getContext().getIdFormatter();

        for (Map.Entry<Long, List<Object>> entry : serviceDao.getServicesForInstances(ids, idF).entrySet()) {
            Map<String, Object> fields = new HashMap<>();
            fields.put(InstanceConstants.FIELD_SERVICE_IDS, entry.getValue());
            result.put(entry.getKey(), fields);
        }

        for (Map.Entry<Long, List<MountEntry>> entry : volumeDao.getMountsForInstances(ids, idF).entrySet()) {
            Map<String, Object> fields = result.get(entry.getKey());
            if (fields == null) {
                fields = new HashMap<>();
                result.put(entry.getKey(), fields);
            }
            fields.put(InstanceConstants.FIELD_MOUNTS, entry.getValue());
        }

        for (Map.Entry<Long, List<HealthcheckState>> entry : serviceDao.getHealthcheckStatesForInstances(ids, idF)
                .entrySet()) {
            Map<String, Object> fields = result.get(entry.getKey());
            if (fields == null) {
                fields = new HashMap<>();
                result.put(entry.getKey(), fields);
            }
            fields.put(InstanceConstants.FIELD_HEALTHCHECK_STATES, entry.getValue());

        }

        return result;
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Instance) {
            return ((Instance) obj).getId();
        }
        return null;
    }
}
