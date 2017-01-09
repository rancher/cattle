package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.core.addon.ProcessSummary;
import io.cattle.platform.core.dao.ProcessSummaryDao;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.Map;

import javax.inject.Inject;

public class ProcessSummaryManager extends AbstractNoOpResourceManager {

    @Inject
    ProcessSummaryDao processSummaryDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ProcessSummary.class };
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return processSummaryDao.getProcessSummary();
    }

    @Override
    protected Object getByIdInternal(String type, String id, ListOptions options) {
        return null;
    }

}