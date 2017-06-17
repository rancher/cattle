package io.cattle.platform.lock.util;

import io.cattle.platform.lock.definition.BlockingLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.definition.MultiLockDefinition;

public class LockUtils {

    public static String serializeLock(LockDefinition lockDef) {
        if (lockDef == null)
            return null;

        if (lockDef instanceof MultiLockDefinition) {
            StringBuilder buffer = new StringBuilder();
            for (LockDefinition child : ((MultiLockDefinition) lockDef).getLockDefinitions()) {
                if (buffer.length() > 0) {
                    buffer.append(",");
                }
                buffer.append(serializeLock(child));
            }
            return buffer.toString();
        } else {
            return serializeSingleLock(lockDef);
        }
    }

    protected static String serializeSingleLock(LockDefinition lockDef) {
        long blocking = 0;
        String id = lockDef.getLockId();
        if (lockDef instanceof BlockingLockDefinition) {
            blocking = ((BlockingLockDefinition) lockDef).getWait();
        }

        return String.format("%s:%d", id, blocking);
    }
}
