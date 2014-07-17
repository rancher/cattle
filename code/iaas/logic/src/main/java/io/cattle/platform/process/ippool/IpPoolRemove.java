package io.cattle.platform.process.ippool;

import io.cattle.platform.core.constants.IpPoolConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.model.IpPool;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.List;

import javax.inject.Named;

@Named
public class IpPoolRemove extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpPool pool = (IpPool)state.getResource();

        if ( IpPoolConstants.KIND_SUBNET_IP_POOL.equals(pool.getKind()) ) {
            return removeSubnetPool(pool, state);
        }

        return null;
    }

    protected HandlerResult removeSubnetPool(IpPool pool, ProcessState state) {
        List<Subnet> subnets = children(pool, Subnet.class);

        for ( Subnet subnet : subnets ) {
            if ( SubnetConstants.KIND_IP_POOL_SUBNET.equals(subnet.getKind()) ) {
                deactivateThenRemove(subnet, state.getData());
            }
        }

        return null;
    }

}
