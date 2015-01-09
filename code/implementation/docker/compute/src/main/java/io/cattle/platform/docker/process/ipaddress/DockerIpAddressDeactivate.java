package io.cattle.platform.docker.process.ipaddress;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.docker.constants.DockerIpAddressConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

public class DockerIpAddressDeactivate extends AbstractObjectProcessHandler {

    @Override
    public String[] getProcessNames() {
        return new String[] { "ipaddress.deactivate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress ipAddress = (IpAddress) state.getResource();

        if (DockerIpAddressConstants.KIND_DOCKER.equals(ipAddress.getKind())) {
            return new HandlerResult(IP_ADDRESS.ADDRESS, (Object) null).withShouldContinue(true);
        }

        return null;
    }

}
