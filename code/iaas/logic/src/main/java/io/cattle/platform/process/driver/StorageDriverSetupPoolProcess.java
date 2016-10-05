package io.cattle.platform.process.driver;

import static io.cattle.platform.core.model.tables.StorageDriverTable.*;

import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.storage.service.StorageService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StorageDriverSetupPoolProcess extends AbstractObjectProcessHandler {

    @Inject
    GenericResourceDao resourceDao;
    @Inject
    StorageService storageService;

    @Override
    public String[] getProcessNames() {
        return new String[] {"storagedriver.activate", "host.activate", "storagedriver.deactivate"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        if (resource instanceof Host) {
           List<StorageDriver> drivers = objectManager.find(StorageDriver.class,
                   STORAGE_DRIVER.ACCOUNT_ID, ((Host) resource).getAccountId(),
                   STORAGE_DRIVER.REMOVED, null);
           for (StorageDriver driver : drivers) {
               storageService.setupPools(driver);
           }
        } else if (resource instanceof StorageDriver) {
            storageService.setupPools((StorageDriver)resource);
        }
        return null;
    }

}
