package io.cattle.platform.process.externalevent;

import static io.cattle.platform.process.externalevent.ExternalEventConstants.FIELD_STACK;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.STACK_LOCK_NAME;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.TYPE_STACK_DELETE;
import io.cattle.platform.core.dao.EnvironmentDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ExternalStackEventCreate extends AbstractDefaultProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(ExternalStackEventCreate.class);

    @Inject
    EnvironmentDao environmentDao;
    @Inject
    LockManager lockManager;
    @Inject
    SchemaFactory schemaFactory;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final ExternalEvent event = (ExternalEvent) state.getResource();

        if (!ExternalEventConstants.KIND_STACK_EVENT.equals(event.getKind())) {
            return null;
        }

        lockManager.lock(new ExternalEventLock(STACK_LOCK_NAME, event.getAccountId(), event.getExternalId()),
                new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        Map<String, Object> stackData = CollectionUtils
                                .toMap(DataUtils.getFields(event).get(FIELD_STACK));
                        if (stackData.isEmpty()) {
                            log.warn("Empty stack for externalStackEvent: {}.", event);
                            return;
                        }

                        if (StringUtils.equals(event.getEventType(), TYPE_STACK_DELETE)) {
                            deleteStack(event, stackData);
                        }
                    }
                });
        return null;
    }

    void deleteStack(ExternalEvent event, Map<String, Object> stackData) {
        Environment env = environmentDao.getEnvironmentByExternalId(event.getAccountId(), event.getExternalId());
        if (env != null) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, env, null);
        }
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { ExternalEventConstants.KIND_EXTERNAL_EVENT + ".create" };
    }
}
