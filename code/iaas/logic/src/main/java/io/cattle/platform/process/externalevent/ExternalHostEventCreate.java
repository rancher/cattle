package io.cattle.platform.process.externalevent;

import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class ExternalHostEventCreate extends AbstractObjectProcessHandler {

    @Inject
    AllocationHelper allocationHelper;

    @Inject
    InstanceDao instanceDao;

    @Inject
    ObjectProcessManager processManager;

    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ExternalEvent event = (ExternalEvent)state.getResource();

        if (!ExternalEventConstants.KIND_EXTERNAL_HOST_EVENT.equals(event.getKind()) ||
                !ExternalEventConstants.TYPE_HOST_EVACUATE.equals(event.getEventType())) {
            return null;
        }

        boolean delete = DataAccessor.fieldBool(event, ExternalEventConstants.FIELD_DELETE_HOST);
        List<Long> hosts = getHosts(event);

        for (long hostId : hosts) {
            Host host = objectManager.loadResource(Host.class, hostId);
            if (host == null) {
                continue;
            }

            try {
                deactivateHost(state, host);
            } catch (ProcessCancelException e) {
            }

            if (delete) {
                host = objectManager.reload(host);
                try {
                    remove(host, state.getData());
                } catch (ProcessCancelException e) {
                }
            }
        }

        return null;
    }

    protected List<Long> getHosts(ExternalEvent event) {
        List<Long> hosts = new ArrayList<>();

        String label = DataAccessor.fieldString(event, ExternalEventConstants.FIELD_HOST_LABEL);
        if (StringUtils.isNotBlank(label)) {
            Map<String, String> labels = new HashMap<>();
            labels.put(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL, DataAccessor.fieldString(event, ExternalEventConstants.FIELD_HOST_LABEL));
            hosts.addAll(allocationHelper.getAllHostsSatisfyingHostAffinity(event.getAccountId(), labels));
        }

        Long hostId = DataAccessor.fieldLong(event, ExternalEventConstants.FIELD_HOST_ID);
        if (hostId != null) {
            hosts.add(hostId);
        }

        return hosts;
    }

    protected void deactivateHost(ProcessState state, Host host) {
        deactivate(host, null);

        List<? extends Instance> instances = instanceDao.getNonRemovedInstanceOn(host.getId());
        List<Instance> removed = new ArrayList<>();
        for (Instance instance : instances ) {
            if (InstanceConstants.isSystem(instance)) {
                continue;
            }
            try {
                processManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance,
                        ProcessUtils.chainInData(new HashMap<String, Object>(), InstanceConstants.PROCESS_STOP,
                                InstanceConstants.PROCESS_REMOVE));
            } catch (ProcessCancelException e) {
            }

            removed.add(instance);
        }

        for (Instance instance : removed) {
            resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return obj.getRemoved() != null;
                }

                @Override
                public String getMessage() {
                    return "removed";
                }
            });
        }
    }

    @Override
    public String[] getProcessNames() {
        return new String[] {ExternalEventConstants.PROCESS_EXTERNAL_EVENT_CREATE};
    }

}