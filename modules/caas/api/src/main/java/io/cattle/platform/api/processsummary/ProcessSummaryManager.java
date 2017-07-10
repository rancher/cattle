package io.cattle.platform.api.processsummary;

import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.core.dao.ProcessSummaryDao;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;

import java.util.Map;

public class ProcessSummaryManager extends AbstractNoOpResourceManager {

    ProcessSummaryDao processSummaryDao;

    public ProcessSummaryManager(ProcessSummaryDao processSummaryDao) {
        this.processSummaryDao = processSummaryDao;
    }

    @Override
    public Object list(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return processSummaryDao.getProcessSummary();
    }

    @Override
    public Object getById(String type, String id, ListOptions options) {
        return null;
    }

}