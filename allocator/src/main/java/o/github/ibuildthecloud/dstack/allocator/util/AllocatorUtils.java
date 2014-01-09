package o.github.ibuildthecloud.dstack.allocator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;

public class AllocatorUtils {

    private static final Logger log = LoggerFactory.getLogger(AllocatorUtils.class);

    public static Boolean checkState(long resourceId, String state, String logType) {
        if ( CommonStatesConstants.ACTIVE.equals(state) ) {
            log.info("{} [{}] is already allocated", logType, resourceId);
            return true;
        } else if ( ! CommonStatesConstants.ACTIVATING.equals(state) ) {
            log.error("Can not allocate {} [{}] in state [{}]", logType, resourceId, state);
            return false;
        }

        return null;
    }

}
