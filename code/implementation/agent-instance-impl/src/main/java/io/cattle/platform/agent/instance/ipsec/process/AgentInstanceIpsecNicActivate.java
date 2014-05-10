package io.cattle.platform.agent.instance.ipsec.process;

import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

public class AgentInstanceIpsecNicActivate extends AbstractObjectProcessLogic implements ProcessPreListener {

    ResourcePoolManager poolManager;
    NetworkDao networkDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic)state.getResource();
        Instance instance = loadResource(Instance.class, nic.getInstanceId());

        if ( nic.getDeviceNumber() == null || nic.getDeviceNumber() != 0 ||
                instance == null || instance.getAgentId() == null ) {
            return null;
        }

        List<? extends NetworkService> service = networkDao.getAgentInstanceNetworkService(instance.getId(),
                NetworkServiceConstants.KIND_IPSEC_TUNNEL);

        if ( service.size() == 0 ) {
            return null;
        }

        Map<Long,Map<String,Integer>> portMap = new HashMap<Long,Map<String,Integer>>();

        for ( Host host : mappedChildren(instance, Host.class) ) {
            List<PooledResource> resources = poolManager.allocateResource(host, instance, new PooledResourceOptions()
                    .withCount(2)
                    .withQualifier(ResourcePoolConstants.HOST_PORT));

            if ( resources == null ) {
                throw new ExecutionException("Failed to allocation ports for ipsec", "Failed to allocate network resources", instance);
            }

            Set<Integer> ports = new TreeSet<Integer>();
            for ( PooledResource resource : resources ) {
                ports.add(new Integer(resource.getName()));
            }

            Iterator<Integer> portIter = ports.iterator();
            Map<String,Integer> hostPortMap = new HashMap<String, Integer>();

            hostPortMap.put("isakmp", portIter.next());
            hostPortMap.put("nat", portIter.next());

            /* Right now we are going to hard code the external ports */
            hostPortMap.put("nat", 4500);
            hostPortMap.put("isakmp", 500);

            portMap.put(host.getId(), hostPortMap);
        }

        objectManager.setFields(instance, ObjectMetaDataManager.APPEND + ObjectMetaDataManager.DATA_FIELD,
                CollectionUtils.asMap("ipsec", portMap));

        return null;
    }

    public ResourcePoolManager getPoolManager() {
        return poolManager;
    }

    @Inject
    public void setPoolManager(ResourcePoolManager poolManager) {
        this.poolManager = poolManager;
    }

    public NetworkDao getNetworkDao() {
        return networkDao;
    }

    @Inject
    public void setNetworkDao(NetworkDao networkDao) {
        this.networkDao = networkDao;
    }

}
