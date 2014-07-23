package io.cattle.platform.process.ipaddress;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpPool;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CreateInstanceAssociatePublicIpAddress extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    IpAddressDao ipAddressDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic)state.getResource();

        if ( nic.getDeviceNumber() == null && nic.getDeviceNumber() != 0 ) {
            return null;
        }

        Instance instance = loadResource(Instance.class, nic.getInstanceId());
        IpPool pool = loadResource(IpPool.class, DataAccessor.fieldLong(instance, InstanceConstants.FIELD_PUBLIC_IP_ADDRESS_POOL_ID));

        if ( pool == null ) {
            return null;
        }

        IpAddress primaryIp = ipAddressDao.getPrimaryIpAddress(nic);

        if ( primaryIp == null ) {
            return null;
        }

        String uuid = primaryIp.getUuid() + "-public-ip";
        IpAddress ip = getObjectManager().findOne(IpAddress.class, ObjectMetaDataManager.UUID_FIELD, uuid);

        if ( ip == null ) {
            Map<String,Object> data = CollectionUtils.asMap(IpAddressConstants.OPTION_RELEASE_ON_CHILD_PURGE, true);
            ip = ipAddressDao.createIpAddressFromPool(pool,
                    ObjectMetaDataManager.UUID_FIELD, uuid,
                    ObjectMetaDataManager.CAPABILITIES_FIELD, Collections.emptyList(),
                    ObjectMetaDataManager.DATA_FIELD, data);
        }

        try {
            createThenActivate(ip, state.getData());
        } catch ( ProcessCancelException e ) {
            // ignore
        }

        Map<String,Object> data = new HashMap<String, Object>();
        data.put(IpAddressConstants.DATA_IP_ADDRESS_ID, primaryIp.getId());
        objectProcessManager.createProcessInstance(IpAddressConstants.PROCESS_IP_ASSOCIATE, ip, data).execute();

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

}
