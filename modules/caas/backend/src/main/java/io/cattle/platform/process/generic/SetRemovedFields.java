package io.cattle.platform.process.generic;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.Date;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicLongProperty;

public class SetRemovedFields implements ProcessHandler {

    private static final String REMOVE_DELAY_FORMAT = "object.%s.remove.time.delay.seconds";
    private static final DynamicLongProperty DEFAULT_REMOVE_DELAY = ArchaiusUtil.getLong("object.remove.time.delay.seconds");
    private static final Random RANDOM = new Random();

    ObjectManager objectManager;

    public SetRemovedFields(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        if (!ObjectUtils.hasWritableProperty(resource, ObjectMetaDataManager.REMOVED_FIELD)) {
            return null;
        }

        Date removed = ObjectUtils.getRemoved(resource);
        if (removed == null) {
            removed = new Date();
        }

        Date removeTime = ObjectUtils.getRemoveTime(resource);
        if (removeTime == null) {
            removeTime = new Date(getRemoveTime(resource));
        }

        objectManager.setFields(resource,
                ObjectMetaDataManager.REMOVED_FIELD, removed,
                ObjectMetaDataManager.REMOVE_TIME_FIELD, removeTime);

        return null;
    }

    protected Long getRemoveTime(Object obj) {
        String type = objectManager.getType(obj);

        long delay = 0;

        /* Look up by string to detect null */
        String delayString = ArchaiusUtil.getString(String.format(REMOVE_DELAY_FORMAT, type)).get();
        if (StringUtils.isBlank(delayString)) {
            delay = DEFAULT_REMOVE_DELAY.get();
        } else {
            delay = Long.parseLong(delayString);
        }

        delay *= 1000;

        if (delay < 0) {
            float f = RANDOM.nextFloat() * delay;
            delay = Math.abs((int)f);
        }

        return System.currentTimeMillis() + delay;
    }

}
