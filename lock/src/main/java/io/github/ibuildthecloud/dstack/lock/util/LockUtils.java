package io.github.ibuildthecloud.dstack.lock.util;

import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.MultiLockDefinition;

public class LockUtils {

    public static String serializeLock(LockDefinition lockDef) {
        if ( lockDef == null )
            return null;

        if ( lockDef instanceof MultiLockDefinition ) {
            StringBuilder buffer = new StringBuilder();
            for ( LockDefinition child : ((MultiLockDefinition)lockDef).getLockDefinitions() ) {
                if ( buffer.length() > 0 ) {
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
        if ( lockDef instanceof BlockingLockDefinition ) {
            blocking = ((BlockingLockDefinition)lockDef).getWait();
        }

        return String.format("%s:%d", id, blocking);
    }
}
