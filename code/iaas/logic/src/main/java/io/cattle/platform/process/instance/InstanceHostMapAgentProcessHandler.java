package io.cattle.platform.process.instance;

import io.cattle.iaas.cluster.service.ClusterManager;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceHostMapRecord;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceHostMapAgentProcessHandler extends AgentBasedProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(InstanceHostMapAgentProcessHandler.class);

    private static final String CLUSTER_CONNECTION_FIELD = "clusterConnection";

    @Inject
    ClusterManager clusterManager;

    @Inject
    ClusterHostMapDao clusterHostMapDao;

    @Inject
    IpAddressDao ipAddressDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        // TODO: Hack dataResource
        if (state.getResource() instanceof InstanceHostMapRecord) {
            // Check if host is a cluster.  If so, augment event with cluster connection info so that agent
            // can connect to cluster server instead of regular docker daemon.
            InstanceHostMapRecord instanceHostMap = (InstanceHostMapRecord) state.getResource();
            Long hostId = instanceHostMap.getHostId();
            Host potentialCluster = objectManager.loadResource(Host.class, hostId);
            if (objectManager.isKind(potentialCluster, ClusterConstants.KIND)) {
                Long managingHostId = DataAccessor.fields(potentialCluster).withKey(ClusterConstants.MANAGING_HOST).as(Long.class);
                Integer clusterServerPort = DataAccessor.fields(potentialCluster).withKey(ClusterConstants.CLUSTER_SERVER_PORT).as(Integer.class);

                Instance clusterServerInstance = clusterManager.getClusterServerInstance(potentialCluster);
                IpAddress ipAddress = clusterManager.getClusterServerInstanceIp(clusterServerInstance);

                DataUtils.getWritableFields(instanceHostMap).put(CLUSTER_CONNECTION_FIELD, "http://" + ipAddress.getAddress() + ":" + clusterServerPort);

                instanceHostMap.setHostId(managingHostId);
            }
        }
        return super.handle(state, process);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void postProcessEvent(EventVO<?> event, Event reply, ProcessState state, ProcessInstance process,
            Object eventResource, Object dataResource, Object agentResource) {
        Host actualHost = null;

        List names = (List)CollectionUtils.getNestedValue(reply.getData(),
                "instanceHostMap", "instance", "+data", "dockerContainer", "Names");

        if (names != null) {
            for (Object name : names) {
                if (name == null) continue;

                String[] nameParts = ((String)name).split("/");
                // name has a leading '/' which makes the first namePart always empty
                if (nameParts.length > 2) {
                    actualHost = clusterHostMapDao.findHostByName(((HostRecord)agentResource).getAccountId(), nameParts[1]);
                    break;
                }
            }
        }

        if (actualHost != null) {
            // update instanceHostMap with actual hostId (instead of clusterId)
            InstanceHostMapRecord instanceHostMapRecord = (InstanceHostMapRecord)state.getResource();
            log.info("Instance [{}] was actually deployed to [{}]", instanceHostMapRecord.getInstanceId(), actualHost.getId());

            instanceHostMapRecord.setHostId(actualHost.getId());

            // clear the clusterConnection field from the mapping since it's no longer needed
            // and can become stale if cluster is deleted
            Map<String, Object> newData = new HashMap<String, Object>();
            newData.putAll(instanceHostMapRecord.getData());
            ((Map)newData.get("fields")).remove(CLUSTER_CONNECTION_FIELD);
            instanceHostMapRecord.setData(newData);

            objectManager.persist(instanceHostMapRecord);
        }
    }
}
