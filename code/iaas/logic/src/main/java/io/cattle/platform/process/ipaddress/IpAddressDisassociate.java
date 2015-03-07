package io.cattle.platform.process.ipaddress;

import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAssociation;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class IpAddressDisassociate extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress address = (IpAddress) state.getResource();

        for (IpAssociation assoc : getObjectManager().children(address, IpAssociation.class, IpAddressConstants.FIELD_IP_ADDRESS_ID)) {
            deactivateThenRemove(assoc, state.getData());
        }

        return null;
    }

}
