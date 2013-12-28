package io.github.ibuildthecloud.dstack.process.common.generic;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.impl.AbstractStatesBasedProcessState;
import io.github.ibuildthecloud.dstack.engine.process.impl.ResourceStatesDefinition;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.process.common.lock.ResourceChangeLock;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class GenericResourceProcessState extends AbstractStatesBasedProcessState {


    Object resource;
    String resourceId;
    ObjectManager objectManager;
    LockDefinition processLock;

    public GenericResourceProcessState(JsonMapper jsonMapper, ResourceStatesDefinition stateDef, LaunchConfiguration config, ObjectManager objectManager) {
        super(jsonMapper, stateDef);
        this.objectManager = objectManager;
        this.resource = objectManager.loadResource(config.getResourceType(), config.getResourceId());
        this.processLock = new ResourceChangeLock(config.getResourceType(), config.getResourceId());
        this.resourceId = config.getResourceId();
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
            if ( resource == null ) {
                return null;
            }
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

    @Override
    public void applyData(Map<String, Object> data) {
        resource = objectManager.setFields(resource, data);
    }

    @Override
    protected Map<String, Object> convertMap(Map<Object, Object> data) {
        return objectManager.convertToPropertiesFor(resource, data);
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public boolean shouldCancel() {
        if ( resource == null ) {
            return true;
        }
        return super.shouldCancel();
    }

}
