package io.cattle.platform.api.data;

import io.cattle.platform.api.resource.DefaultResourceManager;
import io.cattle.platform.api.resource.DefaultResourceManagerSupport;
import io.cattle.platform.core.model.tables.DataTable;

import java.util.Map;

public class DataManager extends DefaultResourceManager {


    public DataManager(DefaultResourceManagerSupport support) {
        super(support);
    }

    @Override
    public Map<Object, Object> getDefaultCriteria(boolean byId, boolean byLink, String type) {
        Map<Object, Object> criteria = super.getDefaultCriteria(byId, byLink, type);
        criteria.put(DataTable.DATA.VISIBLE, true);

        return criteria;
    }

}
