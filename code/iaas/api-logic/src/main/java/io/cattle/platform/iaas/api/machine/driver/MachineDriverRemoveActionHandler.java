package io.cattle.platform.iaas.api.machine.driver;

import static io.cattle.platform.core.model.tables.DynamicSchemaTable.DYNAMIC_SCHEMA;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.List;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class MachineDriverRemoveActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof MachineDriver)) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "NotMachineDriver");
        }
        MachineDriver driver = (MachineDriver) obj;
        List<DynamicSchema> dynamicSchemas = objectManager.find(DynamicSchema.class, DYNAMIC_SCHEMA.NAME,
                StringUtils.removeStart(driver.getName(), "docker-machine-driver-") + "Config");
        for (DynamicSchema dynamicSchema :dynamicSchemas) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, dynamicSchema, null);
        }
        driver.setState(CommonStatesConstants.PURGED);
        return objectManager.reload(driver);
    }

    @Override
    public String getName() {
        return "machinedriver.remove";
    }
}
