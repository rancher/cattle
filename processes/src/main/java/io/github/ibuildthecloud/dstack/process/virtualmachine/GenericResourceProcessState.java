package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.impl.AbstractStatesBasedProcessState;
import io.github.ibuildthecloud.dstack.engine.process.impl.ResourceStatesDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.process.lock.ResourceChangeLock;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;

public class GenericResourceProcessState extends AbstractStatesBasedProcessState {

    String resourceType;
    String resourceId;

    Object resource;
    ObjectManager objectManager;
    LockDefinition processLock;

    public GenericResourceProcessState(ResourceStatesDefinition stateDef, LaunchConfiguration config, ObjectManager objectManager) {
        super(stateDef);
        this.objectManager = objectManager;
        this.resource = objectManager.loadResource(config.getResourceType(), config.getResourceId());
        this.processLock = new ResourceChangeLock(config.getResourceType(), config.getResourceId());
    }

    @Override
    public Object getResource() {
        return resource;
    }

    @Override
    public LockDefinition getProcessLock() {
        return processLock;
    }

    @Override
    public String getState() {
        try {
            return BeanUtils.getProperty(resource, getStatesDefinition().getStateField());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void reload() {
        resource = objectManager.reload(resource);
    }

    @Override
    protected boolean setState(String oldState, String newState) {
        Object newResource = objectManager.setFields(resource, getStatesDefinition().getStateField(), newState);
        if ( newResource == null ) {
            return false;
        } else {
            resource = newResource;
            return true;
        }
    }

}
