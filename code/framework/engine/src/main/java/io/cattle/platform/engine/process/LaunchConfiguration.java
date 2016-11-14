package io.cattle.platform.engine.process;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LaunchConfiguration {

    String processName;
    String resourceType;
    String resourceId;
    Map<String, Object> data;
    Predicate predicate;
    ProcessState parentProcessState;
    Date runAfter;

    public LaunchConfiguration() {
    }

    public LaunchConfiguration(String processName, String resourceType, String resourceId) {
        this(processName, resourceType, resourceId, new HashMap<String, Object>());
    }

    public LaunchConfiguration(String processName, String resourceType, String resourceId, Map<String, Object> data) {
        this.processName = processName;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.data = data;
    }

    public LaunchConfiguration(LaunchConfiguration config) {
        this.processName = config.getProcessName();
        this.resourceType = config.getResourceType();
        this.resourceId = config.getResourceId();
        this.data = config.getData();
        this.predicate = config.getPredicate();
        this.parentProcessState = config.getParentProcessState();
        this.runAfter = config.getRunAfter();
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    @Override
    public String toString() {
        return "LaunchConfiguration [processName=" + processName + ", resourceType=" + resourceType + ", resourceId=" + resourceId + ", data=" + data + "]";
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public ProcessState getParentProcessState() {
        return parentProcessState;
    }

    public void setParentProcessState(ProcessState parentProcessState) {
        this.parentProcessState = parentProcessState;
    }

    public Date getRunAfter() {
        return runAfter;
    }

    public void setRunAfter(Date runAfter) {
        this.runAfter = runAfter;
    }

}
