package io.cattle.platform.process.port;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstancePortCreate extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    NetworkDao ntwkDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        List<String> portDefs = DataUtils.getFieldList(instance.getData(), InstanceConstants.FIELD_PORTS, String.class);
        if (portDefs == null) {
            return null;
        }
        List<Port> toCreate = new ArrayList<>();
        Map<String, Port> toRetain = new HashMap<>();
        ntwkDao.updateInstancePorts(instance, portDefs, toCreate, new ArrayList<Port>(), toRetain);
        for (Port port : toCreate) {
            port = objectManager.create(port);
        }

        for (Port port : toRetain.values()) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, port, state.getData());
        }
        return null;
    }


    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
