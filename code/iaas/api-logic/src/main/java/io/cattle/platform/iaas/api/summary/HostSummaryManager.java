package io.cattle.platform.iaas.api.summary;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.addon.HostSummary;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.Map;

import javax.inject.Inject;

public class HostSummaryManager extends AbstractNoOpResourceManager {

    @Inject
    HostDao hostDao;


    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { HostSummary.class };
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria,
            ListOptions options) {
        Object id = criteria.get(ObjectMetaDataManager.ID_FIELD);
        if ( id != null ) {
            try {
                return hostDao.listSummaries(new Long(id.toString()), ApiUtils.getPolicy().getAccountId());
            } catch ( NumberFormatException nfe ) {
                return null;
            }
        } else {
            return hostDao.listSummaries(null, ApiUtils.getPolicy().getAccountId());
        }
    }

}
