package io.cattle.platform.engine.manager.impl.jooq;

import io.cattle.platform.core.model.tables.records.ProcessInstanceRecord;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessResult;
import io.cattle.platform.engine.process.log.ProcessLog;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.EnumUtils;

public class JooqProcessRecord extends ProcessRecord {

    ProcessInstanceRecord processInstance;

    public JooqProcessRecord(ProcessInstanceRecord record) {
        this.processInstance = record;

        id = record.getId();
        startTime = toTimestamp(record.getStartTime());
        endTime = toTimestamp(record.getEndTime());
        processLog = new ProcessLog();
        result = EnumUtils.getEnum(ProcessResult.class, record.getResult());
        exitReason = EnumUtils.getEnum(ExitReason.class, record.getExitReason());
        startProcessServerId = record.getStartProcessServerId();
        runningProcessServerId = record.getRunningProcessServerId();
        executionCount = record.getExecutionCount();
        runAfter = record.getRunAfter();

        accountId = record.getAccountId();
        priority = record.getPriority();
        resourceType = record.getResourceType();
        resourceId = record.getResourceId();
        processName = record.getProcessName();
        data = new HashMap<>(record.getData());

    }

    protected static Timestamp toTimestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    @Override
    public void setExitReason(ExitReason exitReason) {
        processInstance.setExitReason(exitReason == null ? null : exitReason.toString());
        super.setExitReason(exitReason);
    }

    @Override
    public void setExecutionCount(long executionCount) {
        processInstance.setExecutionCount(executionCount);
        super.setExecutionCount(executionCount);
    }

    @Override
    public void setRunningProcessServerId(String runningProcessServerId) {
        processInstance.setRunningProcessServerId(runningProcessServerId);
        super.setRunningProcessServerId(runningProcessServerId);
    }

    @Override
    public void setEndTime(Date endTime) {
        processInstance.setEndTime(endTime);
        super.setEndTime(endTime);
    }

    @Override
    public void setResult(ProcessResult result) {
        processInstance.setResult(result == null ? null : result.toString());
        super.setResult(result);
    }

    @Override
    public void setRunAfter(Date runAfter) {
        processInstance.setRunAfter(runAfter);
        super.setRunAfter(runAfter);
    }

    public ProcessInstanceRecord getProcessInstance() {
        return processInstance;
    }

}
