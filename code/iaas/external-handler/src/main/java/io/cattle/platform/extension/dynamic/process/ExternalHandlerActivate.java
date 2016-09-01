package io.cattle.platform.extension.dynamic.process;

import static io.cattle.platform.core.model.tables.ExternalHandlerExternalHandlerProcessMapTable.EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP;
import static io.cattle.platform.core.model.tables.ExternalHandlerProcessTable.EXTERNAL_HANDLER_PROCESS;
import io.cattle.platform.core.constants.ExternalHandlerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.ExternalHandler;
import io.cattle.platform.core.model.ExternalHandlerExternalHandlerProcessMap;
import io.cattle.platform.core.model.ExternalHandlerProcess;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.extension.dynamic.api.addon.ExternalHandlerProcessConfig;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ExternalHandlerActivate extends AbstractDefaultProcessHandler {

    LockManager lockManager;
    GenericMapDao mapDao;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ExternalHandler externalHandler = (ExternalHandler) state.getResource();

        Map<String, String> processConfigs = new HashMap<String, String>();
        DataAccessor accessor = DataAccessor.fields(externalHandler).withKey(ExternalHandlerConstants.FIELD_PROCESS_CONFIGS);

        List<? extends ExternalHandlerProcessConfig> list = accessor.asList(jsonMapper, ExternalHandlerProcessConfig.class);
        if (list != null) {
            for (ExternalHandlerProcessConfig config : list) {
                String name = config.getName();
                for (String part : name.toString().trim().split("\\s*,\\s*")) {
                    /* Handle migration from v1 to v2 api */
                    if (part.startsWith("environment.")) {
                        part = part.replace("environment.", "stack.");
                    }
                    processConfigs.put(part, config.getOnError());
                }
            }
        }

        if (!processConfigs.isEmpty()) {
            for (ExternalHandlerExternalHandlerProcessMap map : getObjectManager().children(externalHandler, ExternalHandlerExternalHandlerProcessMap.class)) {
                ExternalHandlerProcess handlerProcess = getObjectManager().loadResource(ExternalHandlerProcess.class, map.getExternalHandlerProcessId());
                processConfigs.remove(handlerProcess.getName());
            }
        }

        if (!processConfigs.isEmpty()) {
            for (Iterator<String> iter = processConfigs.keySet().iterator(); iter.hasNext();) {
                final String processName = iter.next();
                ExternalHandlerProcess handlerProcess = lockManager.lock(new CreateExternalHandlerProcessLock(processName),
                        new LockCallback<ExternalHandlerProcess>() {
                            @Override
                            public ExternalHandlerProcess doWithLock() {
                                return getExternalHandlerProcess(processName);
                            }
                        });

                getObjectManager().create(ExternalHandlerExternalHandlerProcessMap.class, EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_ID,
                        externalHandler.getId(), EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_PROCESS_ID, handlerProcess.getId(),
                        EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.ON_ERROR, processConfigs.get(processName));
            }
        }

        for (ExternalHandlerExternalHandlerProcessMap map : mapDao.findNonRemoved(ExternalHandlerExternalHandlerProcessMap.class, ExternalHandler.class,
                externalHandler.getId())) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, map, state.getData());
        }

        accessor.set(null);
        getObjectManager().persist(externalHandler);

        return null;
    }

    protected ExternalHandlerProcess getExternalHandlerProcess(String name) {
        List<ExternalHandlerProcess> processes = getObjectManager().find(ExternalHandlerProcess.class, EXTERNAL_HANDLER_PROCESS.NAME, name);

        ExternalHandlerProcess process = null;

        if (processes.size() == 0) {
            process = getObjectManager().create(ExternalHandlerProcess.class, EXTERNAL_HANDLER_PROCESS.NAME, name);
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
