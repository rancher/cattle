package io.cattle.platform.iaas.api.dashboard;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.IdFormatterUtils;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class DashboardResourceManager extends AbstractNoOpResourceManager {

    @Inject
    DashBoardDao dashBoardDao;

    @Inject
    IdFormatter idformatter;

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
        Map<String, Map<String, Long>> states = new HashMap<>();
        Map<String, Long> hostStates = new HashMap<>();
        for (Host host: hosts) {
            Long count = hostStates.get(host.getState());
            if (count == null) {
                count = 0L;
            }
            count++;
            hostStates.put(host.getState(), count);
        }
        states.put("hosts", hostStates);
        Map<String, Long> serviceStates = new HashMap<>();
        for (Service service: services) {
            Long count = serviceStates.get(service.getState());
            if (count == null) {
                count = 0L;
            }
            count++;
            serviceStates.put(service.getState(), count);
        }
        states.put("services", serviceStates);
        Map<String, Long> stackStates = new HashMap<>();
        for (Environment stack: stacks) {
            Long count = stackStates.get(stack.getState());
            if (count == null) {
                count = 0L;
            }
            count++;
            stackStates.put(stack.getState(), count);
        }
        states.put("stacks", stackStates);
        Map<String, Long> containerStates = new HashMap<>();
        for (Instance containter: containers) {
            Long count = containerStates.get(containter.getState());
            if (count == null) {
                count = 0L;
            }
            count++;
            containerStates.put(containter.getState(), count);
        }
        states.put("containers", containerStates);
        return states;
    }

    @SuppressWarnings("unchecked")
    private HostInfo getHostInfo(List<Host> hosts, int numBuckets) {
        List<Bucket> cores = newListOfBuckets(numBuckets);
        long totalCores = 0L;
        for (Host host : hosts) {
            for (Double corePercent :
                    (List<Double>) ObjectUtils.getValue(
                            ObjectUtils.getValue(
                                    ObjectUtils.getValue(host, "info"), "cpuInfo"), "cpuCoresPercentages")) {
                for (Bucket bucket : cores) {
                    if (bucket.addValue(
                            corePercent ,
                            String.valueOf(IdFormatterUtils.getFormatter(idformatter).formatId(host.getKind(), host.getId())))) {
                        break;
                    }
                }
                totalCores++;
            }
        }
        List<Bucket> memory = newListOfBuckets(numBuckets);
        double memoryUsed = 0;
        double totalMemory = 0;
        for (Host host : hosts) {
            Map<String, Double> memInfo = ((Map<String, Double>) ObjectUtils.getValue(ObjectUtils.getValue(host, "info"), "memoryInfo"));
            Double memTotal = memInfo.get("memTotal");
            Double memFree = memInfo.get("memFree");
            Double buffers = memInfo.get("buffers");
            Double cached = memInfo.get("cached");
            Double memUsed = ((memTotal-memFree-buffers-cached)/memTotal) * 100;
            memoryUsed += memTotal-memFree-buffers-cached;
            totalMemory += memTotal;
            for (Bucket bucket : memory) {
                if (bucket.addValue(
                        clampPercent(memUsed),
                        String.valueOf(IdFormatterUtils.getFormatter(idformatter).formatId(host.getKind(), host.getId())))) {
                    break;
                }
            }
        }
        List<Bucket> mounts = newListOfBuckets(numBuckets);
        double diskTotal = 0;
        double diskUsed = 0;
        for (Host host : hosts) {
            for (Map<String, Double> mount: ((Map<String, Map<String, Double>>)
                    ObjectUtils.getValue(ObjectUtils.getValue(ObjectUtils.getValue(host, "info"), "diskInfo"), "mountPoints")).values()) {
                diskTotal += mount.get("total");
                diskUsed += mount.get("used");
                for (Bucket bucket : mounts) {
                    if (bucket.addValue(clampPercent(mount.get("percentUsed")) ,
                            String.valueOf(IdFormatterUtils.getFormatter(idformatter).formatId(host.getKind(), host.getId())))) {
                        break;
                    }
                }
            }
        }
        List<Bucket> networkIn = newListOfBuckets(numBuckets);
        putHostsInBuckets(networkIn, hosts);
        List<Bucket> networkOut = newListOfBuckets(numBuckets);
        return new HostInfo(cores, memory, mounts, networkIn, networkOut, 0, memoryUsed, totalMemory, diskUsed, diskTotal, totalCores);
    }

    private void putHostsInBuckets(List<Bucket> buckets, List<Host> hosts) {

    }

    private Double clampPercent(Double toClamp) {
        return Math.max(0, Math.min(100, toClamp));
    }

    private List<Bucket> newListOfBuckets(int numBuckets) {
        List<Bucket> buckets = new ArrayList<>();
        float sizeOfBuckets = 100 / numBuckets;
        for (int i = 0; i < numBuckets; i++) {
            float start = sizeOfBuckets * i;
            float end = sizeOfBuckets * (i + 1);
            buckets.add(new Bucket(start, end, new ArrayList<String>()));
        }
        return buckets;
    }
}
