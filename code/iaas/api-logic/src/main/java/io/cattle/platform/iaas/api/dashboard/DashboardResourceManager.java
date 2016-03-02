package io.cattle.platform.iaas.api.dashboard;

import static sun.jvm.hotspot.code.CompressedStream.L;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class DashboardResourceManager extends AbstractNoOpResourceManager {

    @Inject
    DashBoardDao dashBoardDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Dashboard.class };
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions
            options) {
        ApiContext context  = ApiContext.getContext();
        if (context == null) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "NullApiContext");
        }
        Policy policy = (Policy) context.getPolicy();
        if (policy == null) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "NullPolicy");
        }
        long accountID = policy.getAccountId();

        int numBuckets = 10;

        List<Host> hosts = dashBoardDao.getAllHosts(accountID);
        List<Service> services = dashBoardDao.getAllServices(accountID);
        List<Environment> stacks = dashBoardDao.getAllStacks(accountID);
        List<Instance> containers = dashBoardDao.getAllContainers(accountID);
        long slowProcesses = dashBoardDao.getSlowProcesses(accountID);
        long nowProcesses = dashBoardDao.getCurrentProcesses(accountID);
        long recentProcesses = dashBoardDao.getRecentProcesses(accountID);
        long recentThreshold = 86400;


        Map<String, Map<String, Long>> states = getStates(hosts, services, stacks, containers);
        ProcessesInfo processInfo = new ProcessesInfo(nowProcesses, slowProcesses, recentProcesses, recentThreshold);
        return Collections.singletonList(new Dashboard(getHostInfo(hosts, numBuckets), states, processInfo, dashBoardDao.getAuditLogs(10)));
    }

    private Map<String, Map<String, Long>> getStates(List<Host> hosts, List<Service> services, List<Environment> stacks, List<Instance> containers) {
        return null;
    }

    private HostInfo getHostInfo(List<Host> hosts, int numBuckets) {
        List<Bucket> cores = newListOfBuckets(numBuckets);
        List<Bucket> memory = newListOfBuckets(numBuckets);
        List<Bucket> mounts = newListOfBuckets(numBuckets);
        List<Bucket> networkIn = newListOfBuckets(numBuckets);
        List<Bucket> networkOut = newListOfBuckets(numBuckets);
        return new HostInfo(cores, memory, mounts, networkIn, networkOut, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private List<Bucket> newListOfBuckets(int numBuckets) {
        List<Bucket> buckets = new ArrayList<Bucket>();
        float sizeOfBuckets = 100 / numBuckets;
        for (int i = 0; i < numBuckets; i++) {
            float start = sizeOfBuckets * i;
            float end = sizeOfBuckets * (i + 1);
            buckets.add(new Bucket(start, end, new ArrayList<String>()));
        }
        return buckets;
    }
}
