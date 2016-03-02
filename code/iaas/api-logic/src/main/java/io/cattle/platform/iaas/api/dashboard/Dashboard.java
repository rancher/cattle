package io.cattle.platform.iaas.api.dashboard;

import io.cattle.platform.core.model.AuditLog;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;
import java.util.Map;

@Type(name = "dashboard")
public class Dashboard {
    private HostInfo host;
    private Map<String,Map<String, Long>> states;
    private ProcessesInfo processes;

    public Dashboard(HostInfo host, Map<String, Map<String, Long>> states, ProcessesInfo processes, List<AuditLog>
            auditLogs) {
        this.host = host;
        this.states = states;
        this.processes = processes;
        this.auditLogs = auditLogs;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }

    public ProcessesInfo getProcesses() {
        return processes;
    }

    public void setProcesses(ProcessesInfo processes) {
        this.processes = processes;
    }

    public Map<String, Map<String, Long>> getStates() {
        return states;
    }

    public void setStates(Map<String, Map<String, Long>> states) {
        this.states = states;
    }

    public HostInfo getHost() {
        return host;
    }

    public void setHost(HostInfo host) {
        this.host = host;
    }

    private List<AuditLog> auditLogs;
}
