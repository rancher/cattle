package io.cattle.platform.iaas.api.dashboard;

import static io.cattle.platform.core.model.tables.AuditLogTable.*;
import static io.cattle.platform.core.model.tables.EnvironmentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ProcessInstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

import org.jooq.Condition;
import org.jooq.impl.DSL;

public class DashBoardDaoImpl extends AbstractJooqDao implements DashBoardDao {
    @Override
    public List<AuditLog> getAuditLogs(int numLogsToGet) {
        return create()
                .selectFrom(AUDIT_LOG)
                .orderBy(AUDIT_LOG.CREATED.desc())
                .limit(numLogsToGet)
                .fetchInto(AuditLog.class);
    }

    @Override
    public List<Host> getAllHosts(long accountID) {
        return create()
                .selectFrom(HOST)
                .where(HOST.ACCOUNT_ID.eq(accountID)
                .and(HOST.REMOVED.isNull()))
                .fetchInto(Host.class);
    }

    @Override
    public List<Service> getAllServices(long accountID) {
        return create()
                .selectFrom(SERVICE)
                .where(SERVICE.ACCOUNT_ID.eq(accountID))
                .fetchInto(Service.class);
    }

    @Override
    public List<Environment> getAllStacks(long accountID) {
        return create()
                .selectFrom(ENVIRONMENT)
                .where(ENVIRONMENT.ACCOUNT_ID.eq(accountID)
                        .and(ENVIRONMENT.REMOVED.isNull()))
                .fetchInto(Environment.class);
    }

    @Override
    public List<Instance> getAllContainers(long accountID) {
        return create()
                .selectFrom(INSTANCE)
                .where(INSTANCE.ACCOUNT_ID.eq(accountID)
                        .and(INSTANCE.KIND.equalIgnoreCase("container")
                        .and(INSTANCE.REMOVED.isNull())))
                .fetchInto(Instance.class);
    }

    @Override
    public long getSlowProcesses() {
        return create().selectFrom(PROCESS_INSTANCE).where(PROCESS_INSTANCE.END_TIME.isNotNull()
                .and("TIMESTAMPDIFF(MINUTE, process_instance.start_time, process_instance.end_time)>=5")
        ).fetchCount();
    }

    @Override
    public long getCurrentProcesses() {
        return create().selectFrom(PROCESS_INSTANCE).where(PROCESS_INSTANCE.END_TIME.isNull()).fetchCount();
    }

    @Override
    public long getRecentProcesses() {
        return create().selectFrom(PROCESS_INSTANCE).fetchCount();
    }
}
