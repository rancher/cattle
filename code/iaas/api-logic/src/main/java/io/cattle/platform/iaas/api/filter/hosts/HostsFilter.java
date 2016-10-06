package io.cattle.platform.iaas.api.filter.hosts;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.MachineConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class HostsFilter extends CachedOutputFilter<HostsFilter.Data> implements Priority {

    @Inject
    HostDao hostDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Host.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] {};
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (request != null && "v1".equals(request.getVersion())) {
            return converted;
        }

        if (original instanceof Host) {
            Host host = (Host)original;
            Data data = getCached(request);
            if (data == null) {
                return converted;
            }
            PhysicalHost physicalHost = data.physicalHosts.get(host.getPhysicalHostId());
            Map<String, Object> fields = converted.getFields();
            fields.put(HostConstants.FIELD_INSTANCE_IDS, data.instancesPerHost.get(host.getId()));

            if (physicalHost != null) {
                Map<String, Object> phFields = DataUtils.getFields(physicalHost);
                for (Map.Entry<String, Object> entry : phFields.entrySet()) {
                    if (entry.getValue() == null || MachineConstants.EXTRACTED_CONFIG_FIELD.equals(entry.getKey())) {
                        continue;
                    }
                    String key = entry.getKey();
                    if (key.equals(MachineConstants.FIELD_DRIVER) || key.endsWith(MachineConstants.CONFIG_FIELD_SUFFIX)) {
                        fields.put(key, entry.getValue());
                    }
                }
            }

            String agentState = host.getAgentState();
            if (CommonStatesConstants.ACTIVE.equals(host.getState()) && StringUtils.isNotBlank(agentState)) {
                fields.put(ObjectMetaDataManager.STATE_FIELD, agentState);
            }
        }
        return converted;
    }

    @Override
    protected HostsFilter.Data newObject(ApiRequest apiRequest) {
        List<Long> ids = getIds(apiRequest);
        Data data = new Data();
        data.instancesPerHost = hostDao.getInstancesPerHost(ids, ApiContext.getContext().getIdFormatter());
        data.physicalHosts = hostDao.getPhysicalHostsForHosts(ids);
        return data;
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Host) {
            return ((Host) obj).getId();
        }
        return null;
    }

    static class Data {
        Map<Long, List<Object>> instancesPerHost;
        Map<Long, PhysicalHost> physicalHosts;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

}
