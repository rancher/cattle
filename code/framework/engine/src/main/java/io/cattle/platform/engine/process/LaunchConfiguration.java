package io.cattle.platform.engine.process;

import java.util.Date;
import java.util.Map;

public class LaunchConfiguration {

    String processName;
    String resourceType;
    String resourceId;
    Object accountId;
    Integer priority;
    Map<String, Object> data;
    Predicate predicate;
    ProcessState parentProcessState;
    Date runAfter;

    public LaunchConfiguration() {
    }

    public LaunchConfiguration(String processName, String resourceType, String resourceId, Object accountId, Integer priority, Map<String, Object> data) {
        this.processName = processName;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.accountId = accountId;
        this.data = data;
        this.priority = priority;
    }

    public LaunchConfiguration(LaunchConfiguration config) {
        this.processName = config.getProcessName();
        this.resourceType = config.getResourceType();
        this.resourceId = config.getResourceId();
        this.data = config.getData();
        this.predicate = config.getPredicate();
        this.parentProcessState = config.getParentProcessState();
        this.runAfter = config.getRunAfter();
        this.accountId = config.getAccountId();
        this.priority = config.getPriority();
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
        return "LaunchConfiguration [processName=" + processName + ", resourceType=" + resourceType + ", resourceId=" + resourceId + ", accountId=" + accountId
                + ", priority=" + priority + ", data=" + data + ", runAfter=" + runAfter + ", predicate=" + predicate + ", parentProcessState="
                + parentProcessState + "]";
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

    public Object getAccountId() {
        return accountId;
    }

    public void setAccountId(Object accountId) {
        this.accountId = accountId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

}
