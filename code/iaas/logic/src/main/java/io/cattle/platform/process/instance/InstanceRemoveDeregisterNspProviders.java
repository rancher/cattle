package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProviderInstanceMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceRemoveDeregisterNspProviders extends AbstractObjectProcessLogic implements ProcessPreListener,
        Priority {
    @Inject
    NetworkDao ntwkDao;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        List<NetworkServiceProviderInstanceMap> maps = ntwkDao.findNspInstanceMaps(
                instance);
        for (NetworkServiceProviderInstanceMap map : maps) {
            deactivateThenRemove(map, state.getData());
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
