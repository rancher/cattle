package io.cattle.platform.sample.data;

import com.netflix.config.DynamicStringListProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.MachineDriverConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.sample.lock.SampleDataLock;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.cattle.platform.core.model.Tables.*;

public class InitialData extends ManagedContextRunnable {

    private static final DynamicStringListProperty DRIVERS = ArchaiusUtil.getList("machine.drivers.default");

    private static final Logger CONSOLE_LOG = LoggerFactory.getLogger("ConsoleStatus");

    ObjectManager objectManager;
    DataDao dataDao;
    GenericResourceDao resourceDao;
    LockManager lockManager;
    TransactionDelegate transaction;

    public InitialData(ObjectManager objectManager, DataDao dataDao, GenericResourceDao resourceDao, LockManager lockManager, TransactionDelegate transaction) {
        this.objectManager = objectManager;
        this.dataDao = dataDao;
        this.resourceDao = resourceDao;
        this.lockManager = lockManager;
        this.transaction = transaction;
    }

    @Override
    public void runInContext() {
        lockManager.lock(new SampleDataLock(), () ->{
            migrate("Accounts", this::accounts);
            migrate("Machine Drivers", this::machineDrivers);
            return null;
        });
    }

    private void migrate(String name, Runnable run) {
        transaction.doInTransaction(() -> {
            String key = ("data." + name).toLowerCase();
            dataDao.getOrCreate(key, true, () -> {
                long start = System.currentTimeMillis();
                run.run();
                CONSOLE_LOG.info("Data migration [{}] {}ms done", name, System.currentTimeMillis()-start);
                return "true";
            });
        });
    }

    private void accounts() {
        Account admin = objectManager.create(Account.class,
                ACCOUNT.NAME, "admin",
                ACCOUNT.KIND, "admin",
                ACCOUNT.STATE, CommonStatesConstants.ACTIVE,
                ACCOUNT.UUID, "admin");

        Cluster defaultCluster = DeferredUtils.nest(() -> resourceDao.createAndSchedule(Cluster.class,
                CLUSTER.NAME, "Default",
                CLUSTER.CREATOR_ID, admin.getId()));

        objectManager.create(Account.class,
                ACCOUNT.NAME, "register",
                ACCOUNT.KIND, "register",
                ACCOUNT.STATE, CommonStatesConstants.ACTIVE,
                ACCOUNT.UUID, "register");
        objectManager.create(Account.class,
                ACCOUNT.NAME, "superadmin",
                ACCOUNT.KIND, "superadmin",
                ACCOUNT.STATE, CommonStatesConstants.INACTIVE,
                ACCOUNT.UUID, "superadmin");
        objectManager.create(Account.class,
                ACCOUNT.NAME, "token",
                ACCOUNT.KIND, "token",
                ACCOUNT.STATE, CommonStatesConstants.ACTIVE,
                ACCOUNT.UUID, "token");
        Account defaultProject = objectManager.create(Account.class,
                ACCOUNT.NAME, "Default",
                ACCOUNT.CLUSTER_ID, defaultCluster.getId(),
                ACCOUNT.KIND, "project",
                ACCOUNT.STATE, CommonStatesConstants.ACTIVE,
                ACCOUNT.UUID, "adminProject");
        objectManager.create(Account.class,
                ACCOUNT.NAME, "System Service",
                ACCOUNT.KIND, "service",
                ACCOUNT.STATE, CommonStatesConstants.ACTIVE,
                ACCOUNT.UUID, "service");

        objectManager.create(ProjectMember.class,
                PROJECT_MEMBER.UUID, "adminMember",
                PROJECT_MEMBER.ACCOUNT_ID, defaultProject.getId(),
                PROJECT_MEMBER.PROJECT_ID, defaultProject.getId(),
                PROJECT_MEMBER.EXTERNAL_ID, admin.getId(),
                PROJECT_MEMBER.EXTERNAL_ID_TYPE, ProjectConstants.RANCHER_ID,
                PROJECT_MEMBER.ROLE, ProjectConstants.OWNER);
    }

    protected void machineDrivers() {
        for (String name : DRIVERS.get()) {
            List<String> values = ArchaiusUtil.getList(String.format("machine.driver.%s", name)).get();
            String activate = values.get(0);
            String url = values.get(1);
            String checksum = "";
            if (values.size() > 2) {
                checksum = values.get(2);
            }
            String uuid = Long.toString((url+checksum+name).hashCode());
            boolean activateDefault = Boolean.parseBoolean(activate);
            boolean builtin = url.equals("local://");

            objectManager.create(MachineDriver.class,
                    MACHINE_DRIVER.UUID, uuid,
                    MACHINE_DRIVER.KIND, MachineDriverConstants.TYPE,
                    ObjectMetaDataManager.NAME_FIELD, name,
                    MachineDriverConstants.FIELD_BUILTIN, builtin,
                    MachineDriverConstants.FIELD_CHECKSUM, checksum,
                    MachineDriverConstants.FIELD_URL, url,
                    MachineDriverConstants.FIELD_DEFAULT_ACTIVE, activateDefault,
                    MACHINE_DRIVER.STATE, CommonStatesConstants.INACTIVE);
        }
    }

}