package io.github.ibuildthecloud.dstack.process.generic;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPostListener;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.util.ObjectUtils;
import io.github.ibuildthecloud.dstack.process.common.handler.AbstractObjectProcessLogic;
import io.github.ibuildthecloud.dstack.util.type.Priority;

import java.util.Date;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicLongProperty;

@Named
public class SetRemovedFields extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    private static final String REMOVE_DELAY_FORMAT = "object.%s.remove.time.delay.seconds";
    private static final DynamicLongProperty DEFAULT_REMOVE_DELAY = ArchaiusUtil.getLong("object.remove.time.delay.seconds");

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        if ( ! ObjectUtils.hasWritableProperty(resource, ObjectMetaDataManager.REMOVED_FIELD) ) {
            return null;
        }

        Date removed = ObjectUtils.getRemoved(resource);
        if ( removed == null ) {
            removed = new Date();
        }

        Date removeTime = ObjectUtils.getRemoveTime(resource);
        if ( removeTime == null ) {
            removeTime = new Date(getRemoveTime(resource));
        }

        return new HandlerResult(
                ObjectMetaDataManager.REMOVED_FIELD, removed,
                ObjectMetaDataManager.REMOVE_TIME_FIELD, removeTime);
    }

    protected Long getRemoveTime(Object obj) {
        String type = getObjectManager().getType(obj);

        long delay = 0;

        /* Look up by string to detect null */
        String delayString = ArchaiusUtil.getString(String.format(REMOVE_DELAY_FORMAT, type)).get();
        if ( StringUtils.isBlank(delayString) ) {
            delay = DEFAULT_REMOVE_DELAY.get();
        } else {
            delay = Long.parseLong(delayString);
        }

        return System.currentTimeMillis() + delay * 1000;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "*.remove" };
    }

}

