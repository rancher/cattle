package io.cattle.platform.allocator.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;

public class AllocatorUtils {

    public static final DynamicLongProperty DEFAULT_COMPUTE = ArchaiusUtil.getLong("instance.compute.default");

    private static final Logger log = LoggerFactory.getLogger(AllocatorUtils.class);

    public static Boolean checkAllocateState(long resourceId, String state, String logType) {
        if (CommonStatesConstants.ACTIVE.equals(state)) {
            log.info("{} [{}] is already allocated", logType, resourceId);
            return true;
        } else if (!CommonStatesConstants.ACTIVATING.equals(state)) {
            log.error("Can not allocate {} [{}] in allocation state [{}]", logType, resourceId, state);
            return false;
        }

        return null;
    }

    public static Boolean checkDeallocateState(long resourceId, String state, String logType) {
        if (CommonStatesConstants.INACTIVE.equals(state)) {
            log.info("{} [{}] is already deallocated", logType, resourceId);
            return true;
        }

        if (!CommonStatesConstants.DEACTIVATING.equals(state)) {
            log.info("Can not deallocate {} [{}], is not in an deactivating allocation state", logType, resourceId);
            return false;
        }

        return null;
    }

    public static long getCompute(Instance instance) {
        if (instance == null) {
            return 0;
        }

        return instance.getCompute() == null ? DEFAULT_COMPUTE.get() : instance.getCompute();
    }

}
