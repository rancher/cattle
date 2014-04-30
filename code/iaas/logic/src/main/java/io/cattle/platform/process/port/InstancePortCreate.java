package io.cattle.platform.process.container;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import static io.cattle.platform.core.model.tables.PortTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

@Named
public class ContainersPortCreate extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        ObjectManager objectManager = getObjectManager();

        if ( ! InstanceConstants.KIND_CONTAINER.equals(instance.getKind()) ) {
            return null;
        }

        List<String> portDefs = DataUtils.getFieldList(instance.getData(), InstanceConstants.FIELD_PORTS, String.class);
        if ( portDefs == null ) {
            return null;
        }

        Map<Integer,Port> ports = new HashMap<Integer, Port>();
        for ( Port port : objectManager.children(instance, Port.class) ) {
            ports.put(port.getPrivatePort(), port);
        }

        for ( String port : portDefs ) {
            PortSpec spec = new PortSpec(port);

            if ( ports.containsKey(spec.getPrivatePort()) ) {
                continue;
            }

            Port portObj = objectManager.create(Port.class,
                    PORT.KIND, PortConstants.KIND_USER,
                    PORT.ACCOUNT_ID, instance.getAccountId(),
                    PORT.INSTANCE_ID, instance.getId(),
                    PORT.PUBLIC_PORT, spec.getPublicPort(),
                    PORT.PRIVATE_PORT, spec.getPrivatePort(),
                    PORT.PROTOCOL, spec.getProtocol());
            ports.put(portObj.getPrivatePort(), portObj);
        }

        for ( Port port : ports.values() ) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, port, state.getData());
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
