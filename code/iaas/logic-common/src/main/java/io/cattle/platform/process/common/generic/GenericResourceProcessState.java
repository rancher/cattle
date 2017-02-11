package io.cattle.platform.process.common.generic;

import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.impl.AbstractStatesBasedProcessState;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.engine.process.impl.ResourceStatesDefinition;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.lock.ResourceChangeLock;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.exception.DataChangedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericResourceProcessState extends AbstractStatesBasedProcessState {

    private static final Logger log = LoggerFactory.getLogger(GenericResourceProcessState.class);

    Object resource;
    String resourceId;
    ObjectManager objectManager;
    LockDefinition processLock;
    Map<String, Object> data;
    String state;

    public GenericResourceProcessState(JsonMapper jsonMapper, ResourceStatesDefinition stateDef, LaunchConfiguration config, ObjectManager objectManager) {
        super(jsonMapper, stateDef);
        this.objectManager = objectManager;
        this.resource = objectManager.loadResource(config.getResourceType(), config.getResourceId());
        this.processLock = new ResourceChangeLock(config.getResourceType(), config.getResourceId());
        this.resourceId = config.getResourceId();
        this.data = config.getData();
        this.state = lookupState();
    }

    @Override
    public Object getResource() {
        return resource;
    }

    @Override
    public LockDefinition getProcessLock() {
        return processLock;
    }

    protected String lookupState() {
        try {
            if (resource == null) {
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
    public String getState() {
        return state;
    }

    @Override
    public void reload() {
        resource = objectManager.reload(resource);
    }

    @Override
    public void rebuild() {
        resource = objectManager.reload(resource);
        state = lookupState();
    }

    @Override
    protected boolean setState(boolean transitioning, String oldState, String newState) {
        reload();

        if (resource != null && transitioning && ObjectMetaDataManager.STATE_FIELD.equals(getStatesDefinition().getStateField())) {
            DataAccessor field = DataAccessor.fields(resource).withKey(ObjectMetaDataManager.TRANSITIONING_FIELD);
            DataAccessor message = DataAccessor.fields(resource).withKey(ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD);

            for (DataAccessor accessor : new DataAccessor[] {field, message}) {
                if (StringUtils.isNotBlank(accessor.as(String.class))) {
                    accessor.remove();
                }
            }
        }

        String resourceState = lookupState();
        if (oldState == null || (!oldState.equals(resourceState) && !newState.equals(resourceState))) {
            return false;
        }

        try {
            Object newResource = objectManager.setFields(resource, getStatesDefinition().getStateField(), newState);
            if (newResource == null) {
                return false;
            } else {
                resource = newResource;
                this.state = lookupState();
                return true;
            }
        } catch (DataChangedException e) {
            return false;
        }
    }

    @Override
    public void applyData(Map<String, Object> data) {
        try {
            resource = objectManager.setFields(resource, data);
        } catch (DataChangedException e) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }
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
    public boolean shouldCancel(ProcessRecord record) {
        if (resource == null) {
            log.error("Resource is null, can't find resource id [{}]", resourceId);
            return true;
        }
        return super.shouldCancel(record);
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }

}
