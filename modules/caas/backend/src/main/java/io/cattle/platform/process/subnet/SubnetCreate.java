package io.cattle.platform.process.subnet;

import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.net.NetUtils;

public class SubnetCreate implements ProcessHandler {

    JsonMapper jsonMapper;

    public SubnetCreate(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Subnet subnet = (Subnet) state.getResource();

        String networkAddress = subnet.getNetworkAddress();
        Integer cidrSize = subnet.getCidrSize();
        String gateway = subnet.getGateway();
        String startAddress = subnet.getStartAddress();
        String endAddress = subnet.getEndAddress();

        if (networkAddress == null) {
            networkAddress = "10.44.0.0/16";
        }

        if (networkAddress.contains("/")) {
            String[] parts = networkAddress.split("/", 2);
            networkAddress = parts[0];
            try {
                cidrSize = Integer.parseInt(parts[1]);
            } catch (NumberFormatException nfe) {
                cidrSize = 24;
            }
        }

        if (gateway == null) {
            gateway = NetUtils.getDefaultGateway(networkAddress, cidrSize);
        }

        if (startAddress == null) {
            startAddress = NetUtils.getDefaultStartAddress(networkAddress, cidrSize);
        }

        if (endAddress == null) {
            endAddress = NetUtils.getDefaultEndAddress(networkAddress, cidrSize);
        }

        return new HandlerResult(
                SUBNET.NETWORK_ADDRESS, networkAddress,
                SUBNET.CIDR_SIZE, cidrSize,
                SUBNET.GATEWAY, gateway,
                SUBNET.START_ADDRESS, startAddress,
                SUBNET.END_ADDRESS, endAddress);
    }

}
