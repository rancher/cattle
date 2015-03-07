package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.core.model.tables.DataTable;

import java.util.Map;

public class DataManager extends AbstractJooqResourceManager {

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Data.class };
    }

    @Override
    protected Map<Object, Object> getDefaultCriteria(boolean byId, boolean byLink, String type) {
        Map<Object, Object> criteria = super.getDefaultCriteria(byId, byLink, type);
        criteria.put(DataTable.DATA.VISIBLE, true);

        return criteria;
    }

}
