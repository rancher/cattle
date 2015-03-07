package io.cattle.platform.process.ippool;

import static io.cattle.platform.core.model.tables.SubnetTable.*;
import io.cattle.platform.core.constants.IpPoolConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.model.IpPool;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.List;

import javax.inject.Named;

@Named
public class IpPoolCreate extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpPool pool = (IpPool) state.getResource();

        if (IpPoolConstants.KIND_SUBNET_IP_POOL.equals(pool.getKind())) {
            return createSubnetPool(pool, state);
        }

        return null;
    }

    protected HandlerResult createSubnetPool(IpPool pool, ProcessState state) {
        List<Subnet> subnets = children(pool, Subnet.class);
        Subnet subnet = subnets.size() > 0 ? subnets.get(0) : null;

        if (subnet == null) {
            subnet = getObjectManager().create(Subnet.class, SUBNET.ACCOUNT_ID, pool.getAccountId(), SUBNET.IP_POOL_ID, pool.getId(), SUBNET.KIND,
                    SubnetConstants.KIND_IP_POOL_SUBNET, SUBNET.GATEWAY, DataAccessor.fieldString(pool, IpPoolConstants.FIELD_GATEWAY), SUBNET.END_ADDRESS,
                    DataAccessor.fieldString(pool, IpPoolConstants.FIELD_END_ADDRESS), SUBNET.START_ADDRESS,
                    DataAccessor.fieldString(pool, IpPoolConstants.FIELD_START_ADDRESS), SUBNET.CIDR_SIZE,
                    DataAccessor.fieldString(pool, IpPoolConstants.FIELD_CIDR_SIZE), SUBNET.NETWORK_ADDRESS,
                    DataAccessor.fieldString(pool, IpPoolConstants.FIELD_NETWORK_ADDRESS));
        }

        createThenActivate(subnet, state.getData());

        return null;
    }

}
