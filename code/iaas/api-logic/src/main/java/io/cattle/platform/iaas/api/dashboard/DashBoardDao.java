package io.cattle.platform.iaas.api.dashboard;

import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

import java.util.List;

public interface DashBoardDao{
    List<AuditLog> getAuditLogs(int numLogsToGet);

    List<Host> getAllHosts(long accountID);

    List<Service> getAllServices(long accountID);

    List<Environment> getAllStacks(long accountID);

    List<Instance> getAllContainers(long accountID);

    long getSlowProcesses(long accountID);

    long getCurrentProcesses(long accountID);

    long getRecentProcesses(long accountID);
}
