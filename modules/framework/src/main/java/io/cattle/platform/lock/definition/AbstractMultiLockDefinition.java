package io.cattle.platform.lock.definition;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractMultiLockDefinition implements MultiLockDefinition {

    LockDefinition[] lockDefinitions;
    String[] ids;

    public AbstractMultiLockDefinition(LockDefinition... lockDefinitions) {
        super();
        this.lockDefinitions = lockDefinitions;
        this.ids = new String[lockDefinitions.length];
        for (int i = 0; i < lockDefinitions.length; i++) {
            if (lockDefinitions[i] instanceof MultiLockDefinition) {
                throw new IllegalArgumentException("Can not nest multi locks");
            }
            this.ids[i] = lockDefinitions[i].getLockId();
        }
    }

    public AbstractMultiLockDefinition(String... ids) {
        this(getLockDefinitions(ids));
    }

    @Override
    public String getLockId() {
        return StringUtils.join(ids, ",");
    }

    @Override
    public LockDefinition[] getLockDefinitions() {
        return lockDefinitions;
    }

    private static final LockDefinition[] getLockDefinitions(String... ids) {
        LockDefinition[] result = new LockDefinition[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = new AbstractLockDefinition.DefaultLockDefinition(ids[i]);
        }

        return result;
    }

}
