package io.cattle.platform.lock.definition;

public class Namespace {

    String name;

    public Namespace(String name) {
        this.name = name;
    }

    public LockDefinition getLockDefinition(LockDefinition lockDef) {
        if (lockDef == null) {
            return null;
        }

        if (lockDef instanceof MultiLockDefinition) {
            return getMultiLockDefinition((MultiLockDefinition) lockDef);
        } else if (lockDef instanceof BlockingLockDefinition) {
            return new DefaultBlockingLockDefinition(lockId(lockDef.getLockId()), ((BlockingLockDefinition) lockDef).getWait());
        } else {
            return new DefaultLockDefinition(lockId(lockDef.getLockId()));
        }

    }

    protected LockDefinition getMultiLockDefinition(MultiLockDefinition lockDef) {
        LockDefinition[] defs = lockDef.getLockDefinitions();
        LockDefinition[] resultDefs = new LockDefinition[defs.length];

        for (int i = 0; i < defs.length; i++) {
            resultDefs[i] = getLockDefinition(getLockDefinition(defs[i]));
        }

        return new DefaultMultiLockDefinition(resultDefs);
    }

    protected String lockId(String id) {
        return name + "/" + id;
    }
}
