package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceGarbageCollectActionHandler implements ActionHandler {

    @Inject
    ServiceDao svcDao;
    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public String getName() {
        return ServiceConstants.PROCESS_SERVICE_GARBAGE_COLLECT;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }

        Service service = (Service) obj;

        for (Instance instance : svcDao.getInstancesToGarbageCollect(service)) {
            try {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, instance,
                        null);
            } catch (ProcessCancelException e) {
                if (instance.getState().equalsIgnoreCase(InstanceConstants.STATE_STOPPING)) {
                    continue;
                }
                objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
            }
        }

        return service;
    }
}

