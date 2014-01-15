package io.github.ibuildthecloud.dstack.process.instance;

import io.github.ibuildthecloud.dstack.core.dao.InstanceDao;
import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.InstanceHostMap;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.process.common.handler.EventBasedProcessHandler;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceDeallocate extends EventBasedProcessHandler {

    InstanceDao instanceDao;

    public InstanceDeallocate() {
        setPriority(DEFAULT);
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Instance instance = (Instance)state.getResource();

        for ( InstanceHostMap map : instanceDao.findNonRemovedInstanceHostMaps(instance.getId()) ) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.REMOVE, map, state.getData());
        }

        return new HandlerResult(result);
    }

    public InstanceDao getInstanceDao() {
        return instanceDao;
    }

    @Inject
    public void setInstanceDao(InstanceDao instanceDao) {
        this.instanceDao = instanceDao;
    }

}
