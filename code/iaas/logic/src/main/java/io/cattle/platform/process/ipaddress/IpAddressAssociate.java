package io.cattle.platform.process.ipaddress;

import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAssociation;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class IpAddressAssociate extends AbstractDefaultProcessHandler {

    IpAddressDao ipAddressDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress address = (IpAddress)state.getResource();
        Object id = state.getData().get(IpAddressConstants.DATA_IP_ADDRESS_ID);

        if ( id == null ) {
            return null;
        }

        IpAddress ip = loadResource(IpAddress.class, id.toString());

        if ( ip == null ) {
            return null;
        }

        IpAssociation association = ipAddressDao.createOrFindAssociation(address,  ip);

        createThenActivate(association, state.getData());

        return null;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

}
