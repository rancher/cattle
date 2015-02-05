package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.HostSummary;

import java.util.List;

public interface HostDao {

    List<HostSummary> listSummaries(Long hostId, long accountId);

}
