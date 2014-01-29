package io.github.ibuildthecloud.dstack.process.instance;

import io.github.ibuildthecloud.dstack.core.dao.GenericMapDao;
import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.InstanceHostMap;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.common.handler.EventBasedProcessHandler;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceDeallocate extends EventBasedProcessHandler {

    GenericMapDao mapDao;

    public InstanceDeallocate() {
        setPriority(DEFAULT);
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Instance instance = (Instance)state.getResource();

        for ( InstanceHostMap map : mapDao.findToRemove(InstanceHostMap.class, Instance.class, instance.getId()) ) {
            remove(map, state.getData());
        }

        return new HandlerResult(result);
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}
