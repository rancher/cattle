package io.github.ibuildthecloud.dstack.extension.dynamic.process;

import static io.github.ibuildthecloud.dstack.core.model.tables.ExternalHandlerExternalHandlerProcessMapTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ExternalHandlerProcessTable.*;
import io.github.ibuildthecloud.dstack.core.constants.ExternalHandlerConstants;
import io.github.ibuildthecloud.dstack.core.dao.GenericMapDao;
import io.github.ibuildthecloud.dstack.core.model.ExternalHandler;
import io.github.ibuildthecloud.dstack.core.model.ExternalHandlerExternalHandlerProcessMap;
import io.github.ibuildthecloud.dstack.core.model.ExternalHandlerProcess;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.lock.LockCallback;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.object.util.DataAccessor;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ExternalHandlerActivate extends AbstractDefaultProcessHandler {

    LockManager lockManager;
    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ExternalHandler externalHandler = (ExternalHandler)state.getResource();

        List<String> eventNames = new ArrayList<String>();
        DataAccessor accessor = DataAccessor.fields(externalHandler)
            .withKey(ExternalHandlerConstants.FIELD_PROCESS_NAMES);

        List<?> list = accessor.as(List.class);
        if ( list != null ) {
            for ( Object obj : accessor.as(List.class) ) {
                for ( String part : obj.toString().trim().split("\\s*,\\s*") ) {
                    eventNames.add(part);
                }
            }
        }

        if ( eventNames.size() > 0 ) {
            for ( ExternalHandlerExternalHandlerProcessMap map :
                getObjectManager().children(externalHandler, ExternalHandlerExternalHandlerProcessMap.class)) {
                ExternalHandlerProcess handlerProcess = getObjectManager().loadResource(ExternalHandlerProcess.class, map.getExternalHandlerProcessId());
                eventNames.remove(handlerProcess.getName());
            }
        }

        if ( eventNames.size() > 0 ) {
            for ( final String eventName : eventNames ) {
                ExternalHandlerProcess handlerProcess = lockManager.lock(new CreateExternalHandlerProcessLock(eventName), new LockCallback<ExternalHandlerProcess>() {
                    @Override
                    public ExternalHandlerProcess doWithLock() {
                        return getExternalHandlerProcess(eventName);
                    }
                });

                getObjectManager().create(ExternalHandlerExternalHandlerProcessMap.class,
                        EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_ID, externalHandler.getId(),
                        EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_PROCESS_ID, handlerProcess.getId());
            }
        }

        for ( ExternalHandlerExternalHandlerProcessMap map :
            mapDao.findNonRemoved(ExternalHandlerExternalHandlerProcessMap.class, ExternalHandler.class, externalHandler.getId()) ) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, map, state.getData());
        }

        accessor.set(null);
        getObjectManager().persist(externalHandler);

        return null;
    }

    protected ExternalHandlerProcess getExternalHandlerProcess(String name) {
        List<ExternalHandlerProcess> processes = getObjectManager().find(ExternalHandlerProcess.class, EXTERNAL_HANDLER_PROCESS.NAME, name);

        ExternalHandlerProcess process = null;

        if ( processes.size() == 0 ) {
            process = getObjectManager().create(ExternalHandlerProcess.class,
                    EXTERNAL_HANDLER_PROCESS.NAME, name);
        } else {
            process = processes.get(0);
        }

        getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, process, null);

        return getObjectManager().reload(process);
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }
}
