package io.cattle.platform.extension.dynamic.impl;

import io.cattle.platform.core.constants.ExternalHandlerConstants;
import io.cattle.platform.core.model.ExternalHandler;
import io.cattle.platform.engine.handler.ProcessLogic;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.extension.dynamic.DynamicExtensionHandler;
import io.cattle.platform.extension.dynamic.dao.ExternalHandlerDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.EventBasedProcessHandler;
import io.cattle.platform.util.type.PriorityUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class ExternalDynamicExtensionHandlerImpl implements DynamicExtensionHandler {

    private static final Pattern PROCESS_PATTERN =
            Pattern.compile("process\\.(.*)\\.(handler|post\\.listener|pre\\.listener)s");
    private static final String[] PREFIX = new String[] { "pre.", "post." };

    ExternalHandlerDao externalHandlerDao;
    EventService eventService;
    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;
    ObjectMetaDataManager objectMetaDataManager;

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getExtensionList(String key, Class<T> type) {
        if ( type != null && ! ProcessLogic.class.isAssignableFrom(type) ) {
            return Collections.emptyList();
        }

        Matcher m = PROCESS_PATTERN.matcher(key);
        if ( ! m.matches() ) {
            return Collections.emptyList();
        }

        String eventName = m.group(1);
        String phase = m.group(2);

        for ( String i : PREFIX ) {
            if ( phase.startsWith(i) ) {
                eventName = i + eventName;
                break;
            }
        }

        List<? extends ExternalHandler> externalHandlers = externalHandlerDao.getExternalHandler(eventName);
        if ( externalHandlers.size() == 0 ) {
            return Collections.emptyList();
        }

        List<Object> result = new ArrayList<Object>(externalHandlers.size());
        for ( ExternalHandler handler : externalHandlers ) {
            result.add(toEventHandler(eventName, handler));
        }

        return (List<T>)result;
    }

    protected Object toEventHandler(String eventName, ExternalHandler handler) {
        Integer retries = DataAccessor.fieldInteger(handler, ExternalHandlerConstants.FIELD_RETRIES);
        Long timeout = DataAccessor.fieldLong(handler, ExternalHandlerConstants.FIELD_TIMEOUT);
        String priorityName = DataAccessor.fieldString(handler, ExternalHandlerConstants.FIELD_PRIORITY_NAME);
        Integer priority = handler.getPriority();

        if ( priority == null ) {
            priority = PriorityUtils.getPriorityFromString(priorityName);
        }

        EventBasedProcessHandler processHandler = new EventBasedProcessHandler(eventService, objectManager,
                objectProcessManager, objectMetaDataManager);

        processHandler.setEventName(String.format("%s;handler=%s", eventName, handler.getName()));
        processHandler.setName(handler.getName());
        processHandler.setPriority(priority);
        processHandler.setRetry(retries);
        processHandler.setTimeoutMillis(timeout);

        return processHandler;
    }

    public ExternalHandlerDao getExternalHandlerDao() {
        return externalHandlerDao;
    }

    @Inject
    public void setExternalHandlerDao(ExternalHandlerDao externalHandlerDao) {
        this.externalHandlerDao = externalHandlerDao;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ObjectProcessManager getObjectProcessManager() {
        return objectProcessManager;
    }

    @Inject
    public void setObjectProcessManager(ObjectProcessManager objectProcessManager) {
        this.objectProcessManager = objectProcessManager;
    }

    public ObjectMetaDataManager getObjectMetaDataManager() {
        return objectMetaDataManager;
    }

    @Inject
    public void setObjectMetaDataManager(ObjectMetaDataManager objectMetaDataManager) {
        this.objectMetaDataManager = objectMetaDataManager;
    }

}
