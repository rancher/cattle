package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public class StartVirtualMachineProcessState implements ProcessState {

    @Override
    public void setActivating() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setActive() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public LockDefinition getProcessLock() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LockDefinition getStateChangeLock() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean shouldCancel() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isInactive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isActivating() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void reload() {
        // TODO Auto-generated method stub
        
    }

}
