package io.github.ibuildthecloud.dstack.iaas.api.manager;

import java.util.Map;

import io.github.ibuildthecloud.dstack.api.resource.jooq.AbstractJooqResourceManager;
import io.github.ibuildthecloud.dstack.core.model.Data;
import io.github.ibuildthecloud.dstack.core.model.tables.DataTable;

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
    protected Map<Object, Object> getDefaultCriteria(boolean byId, String type) {
        Map<Object, Object> criteria = super.getDefaultCriteria(byId, type);
        criteria.put(DataTable.DATA.VISIBLE, true);

        return criteria;
    }

}
