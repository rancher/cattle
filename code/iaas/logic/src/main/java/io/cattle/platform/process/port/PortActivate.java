package io.cattle.platform.process.port;

import static io.cattle.platform.core.model.tables.PortTable.*;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class PortActivate extends AbstractDefaultProcessHandler {

    IpAddressDao ipAddressDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Port port = (Port)state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, port.getInstanceId());

        if (instance == null) {
            return null;
        }

        Long privateIpAddress = port.getPrivateIpAddressId();
        Long publicIpAddress = port.getPublicIpAddressId();

        for (Nic nic : getObjectManager().children(instance, Nic.class)) {
            Integer device = nic.getDeviceNumber();

            if (device != null && device == 0) {
                IpAddress ipAddress = ipAddressDao.getPrimaryIpAddress(nic);
                if (ipAddress != null) {
                    privateIpAddress = ipAddress.getId();
                }
            }
        }

        String bindAddress = DataAccessor.fieldString(port, PortConstants.FIELD_BIND_ADDR);
        if (StringUtils.isBlank(bindAddress)) {
            if (publicIpAddress == null) {
                outer: for (Host host : getObjectManager().mappedChildren(instance, Host.class)) {
                    for (IpAddress ipAddress : getObjectManager().mappedChildren(host, IpAddress.class)) {
                        publicIpAddress = ipAddress.getId();
                        break outer;
                    }
                }
            }
        }

        return new HandlerResult(PORT.PUBLIC_IP_ADDRESS_ID, publicIpAddress, PORT.PRIVATE_IP_ADDRESS_ID, privateIpAddress).withShouldContinue(true);
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

}
