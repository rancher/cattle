package io.cattle.platform.iaas.api.filter.hosts;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class HostsFilter extends CachedOutputFilter<Map<Long, List<Object>>> {

    @Inject
    HostDao hostDao;
    @Inject
    ObjectManager objectManager;

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
        if (original instanceof Host) {
            Map<Long, List<Object>> data = getCached(request);
            if (data != null) {
                converted.getFields().put(HostConstants.FIELD_INSTANCE_IDS, data.get(((Host) original).getId()));
            }
        }
        return converted;
    }

    @Override
    protected Map<Long, List<Object>> newObject(ApiRequest apiRequest) {
        return hostDao.getInstancesPerHost(getIds(apiRequest), ApiContext.getContext().getIdFormatter());
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Host) {
            return ((Host) obj).getId();
        }
        return null;
    }



}
