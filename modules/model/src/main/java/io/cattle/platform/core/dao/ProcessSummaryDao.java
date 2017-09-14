package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.ProcessSummary;

import java.util.List;

public interface ProcessSummaryDao {

    List<ProcessSummary> getProcessSummary();

}
