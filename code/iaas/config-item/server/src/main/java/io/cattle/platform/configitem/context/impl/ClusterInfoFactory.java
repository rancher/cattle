package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.ClusterHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringProperty;

@Named
public class ClusterInfoFactory extends AbstractAgentBaseContextFactory {
    static final DynamicStringProperty CLUSTER_INSECUREDOCKER_PORT = ArchaiusUtil.getString("cluster.insecuredocker.port");

    @Inject
    ClusterHostMapDao clusterHostMapDao;

    @Inject
    ObjectManager objectManager;

    @Override
    protected void populateContext(Agent agent, Instance instance,
            ConfigItem item, ArchiveContext context) {
        Host cluster = getClusterFromAgent(agent);

        Integer clusterServerPort = DataAccessor.fields(cluster).withKey(ClusterConstants.CLUSTER_SERVER_PORT).as(Integer.class);
        String discoverySpec = DataAccessor.fields(cluster).withKey(ClusterConstants.DISCOVERY_SPEC).as(String.class);
        List<ClusterHostMapRecord> clusterHostMaps = clusterHostMapDao.findClusterHostMapsForCluster(cluster);
        List<String> hostPorts = new ArrayList<String>();

        for (ClusterHostMap mapping : clusterHostMaps) {
            if (CommonStatesConstants.REMOVING.equals(mapping.getState())) {
                continue;
            }
            Long hostId = mapping.getHostId();
            // TODO: Optimize with bulk load
            Host host = objectManager.loadResource(Host.class, hostId);
            if (host == null ||
                    !(CommonStatesConstants.ACTIVE.equals(host.getState()) || CommonStatesConstants.ACTIVATING.equals(host.getState()))) {
                continue;
            }
            IpAddress ipAddress = clusterHostMapDao.getIpAddressForHost(hostId);
            hostPorts.add(ipAddress.getAddress() + ":" + CLUSTER_INSECUREDOCKER_PORT.getValue());
        }

        context.getData().put("clusterServerPort", clusterServerPort);
        context.getData().put("discoverySpec", discoverySpec);
        context.getData().put("hosts", hostPorts);

        Long certificateId = DataAccessor.fields(cluster).withKey(ClusterConstants.CERT_REFERENCE).as(Long.class);
        if (certificateId != null) {
            Certificate certs = objectManager.loadResource(Certificate.class, certificateId);
            context.getData().put("cert", certs.getCert());
            context.getData().put("ca", certs.getCertChain());
            context.getData().put("key", certs.getKey());
        }
    }

    // NOTE: It would be nice to move these into ClusterManager; however, looks like we
    // might run into cyclically dependencies with the eclipse projects

    public Host getClusterFromAgent(Agent agent) {
        // get cluster id from agent uri
        String uri = agent.getUri();
        String[] result = uri.split("clusterId|&");
        Long clusterId = Long.valueOf(result[1].substring(result[1].indexOf("=") + 1));
        return objectManager.loadResource(Host.class, clusterId);
    }
}
