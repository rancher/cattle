package io.cattle.platform.api.instance;

import io.cattle.platform.api.common.CachedOutputFilter;
import io.cattle.platform.core.addon.MountEntry;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.transform.DockerTransformer;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstanceOutputFilter extends CachedOutputFilter<Map<Long, Map<String, Object>>> {

    VolumeDao volumeDao;
    DockerTransformer transformer;

    public InstanceOutputFilter(VolumeDao volumeDao, DockerTransformer transformer) {
        super();
        this.volumeDao = volumeDao;
        this.transformer = transformer;
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (request == null) {
            if (original instanceof Instance) {
                Map<Long, Map<String, Object>> data = getCached(request);
                if (data != null) {
                    Map<String, Object> fields = data.get(((Instance) original).getId());
                    if (fields != null) {
                        converted.getFields().putAll(fields);
                    }
                }
                converted.getFields().put(InstanceConstants.FIELD_EXIT_CODE, transformer.getExitCode((Instance) original));
            }
        }

        if (((Instance) original).getServiceId() != null) {
            Map<String, URL> actions = converted.getActions();
            if (actions != null) {
                actions.remove("converttoservice");
            }
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

        for (Map.Entry<Long, List<MountEntry>> entry : volumeDao.getMountsForInstances(ids, idF).entrySet()) {
            Map<String, Object> fields = result.get(entry.getKey());
            if (fields == null) {
                fields = new HashMap<>();
                result.put(entry.getKey(), fields);
            }
            fields.put(InstanceConstants.FIELD_MOUNTS, entry.getValue());
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
