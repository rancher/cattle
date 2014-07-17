package io.cattle.platform.process.ipaddress;

import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAssociation;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class IpAddressPurge extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress ipAddress = (IpAddress)state.getResource();

        for ( IpAssociation assoc : objectManager.children(ipAddress, IpAssociation.class, IpAddressConstants.FIELD_CHILD_IP_ADDRESS_ID) ) {
            IpAddress parentIp = loadResource(IpAddress.class, assoc.getIpAddressId());
            if ( ! IpAddressConstants.KIND_POOLED_IP_ADDRESS.equals(parentIp.getKind()) ) {
                continue;
            }

            Boolean release = DataAccessor.fromDataFieldOf(parentIp)
                                .withKey(IpAddressConstants.OPTION_RELEASE_ON_CHILD_PURGE)
                                .as(Boolean.class);

            if ( release != null && release.booleanValue() ) {
                try {
                    objectProcessManager.createProcessInstance(IpAddressConstants.PROCESS_IP_DISASSOCIATE, parentIp, state.getData()).execute();
                } catch ( ProcessCancelException e ) {
                    //ignore
                }
                parentIp = objectManager.reload(parentIp);
                deactivateThenRemove(parentIp, state.getData());
            }
        }

        return null;
    }

}
