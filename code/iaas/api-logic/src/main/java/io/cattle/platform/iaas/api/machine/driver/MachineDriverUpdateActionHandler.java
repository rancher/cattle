package io.cattle.platform.iaas.api.machine.driver;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.core.model.tables.records.MachineDriverRecord;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class MachineDriverUpdateActionHandler implements ActionHandler{

    @Inject
    ObjectManager objectManager;

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (obj instanceof MachineDriver) {
            MachineDriverUpdateInput machineDriverUpdate = request.proxyRequestObject(MachineDriverUpdateInput.class);
            MachineDriver driver = ((MachineDriverRecord) obj);
            driver.setMd5checksum(machineDriverUpdate.getmd5checksum());
            if (StringUtils.isNotBlank(machineDriverUpdate.getUri())) {
                driver.setName(machineDriverUpdate.getName());
            }
            if (StringUtils.isNotBlank(machineDriverUpdate.getUri())) {
                driver.setUri(machineDriverUpdate.getUri());
            }
            driver.setState(CommonStatesConstants.INACTIVE);
            DataAccessor.fields(obj).withKey(MachineDriverErrorActionHandler.ERROR_MESSAGE)
                    .set(null);
            objectManager.persist(obj);
            return objectManager.reload(obj);
        }
        throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "NotMachineDriver");
    }

    @Override
    public String getName() {
        return "machinedriver.update";
    }
}
