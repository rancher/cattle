package io.cattle.platform.iaas.api.dashboard;

import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class DashBoardDaoImpl extends AbstractJooqDao implements DashBoardDao {
    @Override
    public List<AuditLog> getAuditLogs(int numLogsToGet) {
        return null;
    }

    @Override
    public List<Host> getAllHosts(long accountID) {
        return null;
    }

    @Override
    public List<Service> getAllServices(long accountID) {
        return null;
    }

    @Override
    public List<Environment> getAllStacks(long accountID) {
        return null;
    }

    @Override
    public List<Instance> getAllContainers(long accountID) {
        return null;
    }

    @Override
    public long getSlowProcesses(long accountID) {
        return 0;
    }

    @Override
    public long getCurrentProcesses(long accountID) {
        return 0;
    }

    @Override
    public long getRecentProcesses(long accountID) {
        return 0;
    }
}
