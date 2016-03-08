package io.cattle.platform.iaas.api.machine.driver;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class MachineDriverErrorActionHandler implements ActionHandler{

    static final String ERROR_MESSAGE = "errorMessage";

    @Inject
    ObjectManager objectManager;

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (obj instanceof MachineDriver) {
            MachineDriverErrorInput machineDriverErrorInput = request.proxyRequestObject(MachineDriverErrorInput.class);
            if (StringUtils.isNotBlank(machineDriverErrorInput.getErrorMessage())) {
                DataAccessor.fields(obj).withKey(ERROR_MESSAGE).set(machineDriverErrorInput.getErrorMessage());
                ((MachineDriver) obj).setState(InstanceConstants.STATE_ERROR);
            }
            objectManager.persist(obj);
            return objectManager.reload(obj);
        }
        throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "NotMachineDriver");
    }

    @Override
    public String getName() {
        return "machinedriver.error";
    }
}
