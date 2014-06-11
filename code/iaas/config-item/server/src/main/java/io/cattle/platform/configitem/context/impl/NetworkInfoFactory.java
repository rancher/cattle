package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.NetworkInfoDao;
import io.cattle.platform.configitem.context.data.NetworkServiceInfo;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NetworkInfoFactory extends AbstractAgentBaseContextFactory {

    NetworkInfoDao networkInfo;
    IpAddressDao ipAddressDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        context.getData().put("hostnameGenerator", new HostnameGenerator());
        context.getData().put("networkClients", networkInfo.networkClients(instance));
        context.getData().put("instance", instance);
        context.getData().put("agent", agent);

        List<? extends NetworkService> services = networkInfo.networkServices(instance);
        Map<String,NetworkServiceInfo> servicesMap = new HashMap<String, NetworkServiceInfo>();
        Set<String> serviceSet = new HashSet<String>();

        List<Nic> nics = objectManager.children(instance, Nic.class);
        Network primaryNetwork = null;
        Map<Long,Network> networks = networkInfo.networks(instance);
        Map<String,IpAddress> ipAddresses = new HashMap<String, IpAddress>();

        for ( NetworkService service : services ) {
            serviceSet.add(service.getKind());

            NetworkServiceInfo info = servicesMap.get(service.getKind());
            if ( info == null ) {
                info = new NetworkServiceInfo(service);
                servicesMap.put(service.getKind(), info);
            }

            for ( Nic nic : nics ) {
                if ( ! ipAddresses.containsKey(nic.getUuid()) ) {
                    ipAddresses.put(nic.getUuid(), ipAddressDao.getPrimaryIpAddress(nic));
                }

                if ( primaryNetwork == null && nic.getDeviceNumber() != null && nic.getDeviceNumber() == 0 ) {
                    primaryNetwork = networks.get(nic.getNetworkId());
                }

                if ( service.getNetworkId().equals(nic.getNetworkId()) ) {
                    if ( ! info.getNicIds().contains(nic.getId()) ) {
                        info.getNicIds().add(nic.getId());
                        info.getNics().add(nic);
                    }
                }
            }

            if ( ! info.getNetworkIds().contains(service.getNetworkId()) ) {
                info.getNetworkIds().add(service.getNetworkId());
                info.getNetworks().add(networks.get(service.getNetworkId()));
            }
        }

        context.getData().put("nics", nics);
        context.getData().put("primaryIpAddresses", ipAddresses);
        context.getData().put("services", servicesMap);
        context.getData().put("serviceSet", serviceSet);
        context.getData().put("primaryNetwork", primaryNetwork);
    }

    public NetworkInfoDao getNetworkInfo() {
        return networkInfo;
    }

    @Inject
    public void setNetworkInfo(NetworkInfoDao networkInfo) {
        this.networkInfo = networkInfo;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

}
