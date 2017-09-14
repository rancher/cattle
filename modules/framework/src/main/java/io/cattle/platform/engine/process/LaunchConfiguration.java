package io.cattle.platform.engine.process;

import java.util.Date;
import java.util.Map;

public class LaunchConfiguration {

    protected String processName;
    protected String resourceType;
    protected String resourceId;
    protected Long accountId;
    protected Long clusterId;
    protected Integer priority;
    protected Map<String, Object> data;
    protected ProcessState parentProcessState;
    protected Date runAfter;

    public LaunchConfiguration() {
    }

    public LaunchConfiguration(String processName, String resourceType, String resourceId, Long accountId, Long clusterId, Integer priority, Map<String, Object> data) {
        this.processName = processName;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.accountId = accountId;
        this.clusterId = clusterId;
        this.data = data;
        this.priority = priority;
    }

    public LaunchConfiguration(LaunchConfiguration config) {
        this.processName = config.getProcessName();
        this.resourceType = config.getResourceType();
        this.resourceId = config.getResourceId();
        this.data = config.getData();
        this.parentProcessState = config.getParentProcessState();
        this.runAfter = config.getRunAfter();
        this.accountId = config.getAccountId();
        this.clusterId = config.getClusterId();
        this.priority = config.getPriority();
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getProcessName() {
        return processName;
    }

    @Override
    public String toString() {
        return "LaunchConfiguration{" +
                "processName='" + processName + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", accountId=" + accountId +
                ", clusterId=" + clusterId +
                ", priority=" + priority +
                ", data=" + data +
                ", parentProcessState=" + parentProcessState +
                ", runAfter=" + runAfter +
                '}';
    }

    public ProcessState getParentProcessState() {
        return parentProcessState;
    }

    public Date getRunAfter() {
        return runAfter;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setRunAfter(Date runAfter) {
        this.runAfter = runAfter;
    }

    public void setParentProcessState(ProcessState parentProcessState) {
        this.parentProcessState = parentProcessState;
    }

}
