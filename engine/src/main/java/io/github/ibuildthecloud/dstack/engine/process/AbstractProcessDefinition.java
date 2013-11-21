package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public abstract class AbstractProcessDefinition implements ProcessDefinition {

    private static final Logger log = LoggerFactory.getLogger(AbstractProcessDefinition.class);

    public static final String PRE = "process.%s.pre.listeners";
    public static final String HANDLER = "process.%s.handlers";
    public static final String POST = "process.%s.post.listeners";

    private static final String[] EMPTY = new String[0];

    DynamicStringProperty preProcessListenersSetting;
    DynamicStringProperty processHandlersSetting;
    DynamicStringProperty postProcessListenersSetting;
    ProcessLogicRegistry logicRegistry;

    @PostConstruct
    public void init() {
        preProcessListenersSetting = getSetting(preProcessListenersSetting, PRE);
        processHandlersSetting = getSetting(processHandlersSetting, HANDLER);
        postProcessListenersSetting = getSetting(postProcessListenersSetting, POST);
    }

    protected DynamicStringProperty getSetting(DynamicStringProperty prop, String format) {
        if ( prop != null )
            return prop;
        return ArchaiusUtil.getStringProperty(String.format(format, getName()));
    }

    protected String[] getList(DynamicStringProperty prop) {
        String value = prop.get();
        if ( StringUtils.isBlank(value) )
            return EMPTY;
        return value.trim().split("\\s*,\\s*");
    }

    @Override
    public List<ProcessHandler> getProcessHandlers() {
        String[] names = getList(processHandlersSetting);
        if ( names.length == 0 )
            return Collections.emptyList();

        List<ProcessHandler> result = new ArrayList<ProcessHandler>(names.length);
        for ( String name : names ) {
            ProcessHandler handler = logicRegistry.getProcessHandler(name);
            if ( handler == null ) {
                log.error("Failed to find process handler [{}] for process [{}]", name, getName());
            } else {
                result.add(handler);
            }
        }

        return result;
    }

    protected List<ProcessListener> getListeners(DynamicStringProperty prop) {
        String[] names = getList(prop);
        if ( names.length == 0 )
            return Collections.emptyList();

        List<ProcessListener> result = new ArrayList<ProcessListener>(names.length);
        for ( String name : names ) {
            ProcessListener listener = logicRegistry.getProcessListener(name);
            if ( listener == null ) {
                log.error("Failed to find process listener [{}] for process [{}]", name, getName());
            } else {
                result.add(listener);
            }
        }

        return result;
    }

    @Override
    public List<ProcessListener> getPreProcessListeners() {
        return getListeners(preProcessListenersSetting);
    }

    @Override
    public List<ProcessListener> getPostProcessListeners() {
        return getListeners(postProcessListenersSetting);
    }

    public DynamicStringProperty getPreProcessListenersSetting() {
        return preProcessListenersSetting;
    }

    public void setPreProcessListenersSetting(DynamicStringProperty preProcessListenersSetting) {
        this.preProcessListenersSetting = preProcessListenersSetting;
    }

    public DynamicStringProperty getProcessHandlersSetting() {
        return processHandlersSetting;
    }

    public void setProcessHandlersSetting(DynamicStringProperty processHandlersSetting) {
        this.processHandlersSetting = processHandlersSetting;
    }

    public DynamicStringProperty getPostProcessListenersSetting() {
        return postProcessListenersSetting;
    }

    public void setPostProcessListenersSetting(DynamicStringProperty postProcessListenersSetting) {
        this.postProcessListenersSetting = postProcessListenersSetting;
    }

    public ProcessLogicRegistry getLogicRegistry() {
        return logicRegistry;
    }

    @Inject
    public void setLogicRegistry(ProcessLogicRegistry logicRegistry) {
        this.logicRegistry = logicRegistry;
    }

}
