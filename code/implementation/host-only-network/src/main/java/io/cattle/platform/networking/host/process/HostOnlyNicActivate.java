package io.cattle.platform.networking.host.process;

import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostVnetMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.SubnetVnetMap;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.networking.host.contants.HostOnlyConstants;
import io.cattle.platform.networking.host.dao.HostOnlyDao;
import io.cattle.platform.networking.host.lock.VnetHostCreateLock;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

public class HostOnlyNicActivate extends AbstractObjectProcessLogic implements ProcessPreListener {

    HostOnlyDao hostOnlyDao;
    LockManager lockManager;
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic)state.getResource();
        Network network = loadResource(Network.class, nic.getNetworkId());
        Instance instance = loadResource(Instance.class, nic.getInstanceId());
        Subnet subnet = loadResource(Subnet.class, nic.getSubnetId());

        if ( ! objectManager.isKind(network, HostOnlyConstants.KIND_HOST_ONLY) ) {
            return null;
        }

        Vnet vnet = null;

        for ( Host host : mappedChildren(instance, Host.class) ) {
            vnet = hostOnlyDao.getVnetForHost(network, host);

            if ( vnet == null ) {
                vnet = createVnetForHost(subnet, network, host);
            }

            createIgnoreCancel(vnet, null);

            for ( HostVnetMap map : mapDao.findNonRemoved(HostVnetMap.class, Vnet.class, vnet.getId()) ) {
                if ( map.getHostId().equals(host.getId()) ) {
                    createThenActivate(vnet, state.getData());
                }
            }

            if ( subnet != null ) {
                for ( SubnetVnetMap map : mapDao.findNonRemoved(SubnetVnetMap.class, Vnet.class, vnet.getId()) ) {
                    if ( map.getSubnetId().equals(subnet.getId()) ) {
                        createThenActivate(map, state.getData());
                    }
                }
            }
        }

        if ( vnet == null ) {
            return null;
        } else {
            return new HandlerResult(NIC.VNET_ID, vnet.getId()).withShouldContinue(true);
        }

    }

    protected Vnet createVnetForHost(final Subnet subnet, final Network network, final Host host) {
        final String uri = DataAccessor.field(network, HostOnlyConstants.FIELD_HOST_VNET_URI, String.class);

        return lockManager.lock(new VnetHostCreateLock(network, host), new LockCallback<Vnet>() {
            @Override
            public Vnet doWithLock() {
                Vnet vnet = hostOnlyDao.getVnetForHost(network, host);
                if ( vnet != null ) {
                    return vnet;
                }

                return hostOnlyDao.createVnetForHost(network, host, subnet, uri);
            }
        });
    }

    public HostOnlyDao getHostOnlyDao() {
        return hostOnlyDao;
    }

    @Inject
    public void setHostOnlyDao(HostOnlyDao hostOnlyDao) {
        this.hostOnlyDao = hostOnlyDao;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}