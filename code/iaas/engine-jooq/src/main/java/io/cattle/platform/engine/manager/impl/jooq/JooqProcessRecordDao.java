package io.cattle.platform.engine.manager.impl.jooq;

import static io.cattle.platform.core.model.tables.ProcessExecutionTable.*;
import static io.cattle.platform.core.model.tables.ProcessInstanceTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.ProcessInstance;
import io.cattle.platform.core.model.tables.records.ProcessInstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessPhase;
import io.cattle.platform.engine.process.ProcessResult;
import io.cattle.platform.engine.process.log.ProcessLog;
import io.cattle.platform.engine.server.ProcessInstanceReference;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.Condition;
import org.jooq.Record6;
import org.jooq.RecordHandler;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;

public class JooqProcessRecordDao extends AbstractJooqDao implements ProcessRecordDao {

    private static final Logger log = LoggerFactory.getLogger(JooqProcessRecordDao.class);
    private static final DynamicIntProperty BATCH = ArchaiusUtil.getInt("process.replay.batch.size");

    @Inject
    JsonMapper jsonMapper;
    @Inject
    ObjectManager objectManager;

    @Override
    public List<ProcessInstanceReference> pendingTasks() {
        return pendingTasks(null, null);
    }

    @Override
    public Long nextTask(String resourceType, String resourceId) {
        List<ProcessInstanceReference> refs = pendingTasks(resourceType, resourceId);
        return refs.size() == 0 ? null : refs.get(0).getProcessId();
    }

    protected List<ProcessInstanceReference> pendingTasks(String resourceType, String resourceId) {
        final List<ProcessInstanceReference> result = new ArrayList<ProcessInstanceReference>();
        final Set<String> seen = new HashSet<String>();
        create()
            .select(PROCESS_INSTANCE.ID,
                    PROCESS_INSTANCE.PROCESS_NAME,
                    PROCESS_INSTANCE.RESOURCE_TYPE,
                    PROCESS_INSTANCE.RESOURCE_ID,
                    PROCESS_INSTANCE.ACCOUNT_ID,
                    PROCESS_INSTANCE.PRIORITY)
            .from(PROCESS_INSTANCE)
                .where(processCondition(resourceType, resourceId))
                    .and(runAfterCondition(resourceType))
                .limit(resourceType == null ? BATCH.get() : 1)
                .fetchInto(new RecordHandler<Record6<Long, String, String, String, Long, Integer>>() {
            @Override
            public void next(Record6<Long, String, String, String, Long, Integer> record) {
                String resource = String.format("%s:%s", record.getValue(PROCESS_INSTANCE.RESOURCE_TYPE),
                        record.getValue(PROCESS_INSTANCE.RESOURCE_ID));
                if (seen.contains(resource)) {
                    return;
                }

                ProcessInstanceReference ref = new ProcessInstanceReference();
                ref.setProcessId(record.getValue(PROCESS_INSTANCE.ID));
                ref.setName(record.getValue(PROCESS_INSTANCE.PROCESS_NAME));

                Integer priority = record.getValue(PROCESS_INSTANCE.PRIORITY);
                if (priority != null) {
                    ref.setPriority(priority);
                }

                seen.add(resource);
                result.add(ref);
            }
        });

        return result;
    }

    protected Condition runAfterCondition(String resourceType) {
        if (resourceType == null) {
            return PROCESS_INSTANCE.RUN_AFTER.isNull()
                    .or(PROCESS_INSTANCE.RUN_AFTER.le(new Date()));
        }
        return DSL.trueCondition();
    }

    protected Condition processCondition(String resourceType, String resourceId) {
        if (resourceType == null) {
            return PROCESS_INSTANCE.END_TIME.isNull();
        } else {
            return PROCESS_INSTANCE.END_TIME.isNull().and(PROCESS_INSTANCE.RESOURCE_TYPE.eq(resourceType)).and(PROCESS_INSTANCE.RESOURCE_ID.eq(resourceId));
        }
    }

    @Override
    public ProcessRecord getRecord(Long id) {
        io.cattle.platform.core.model.ProcessInstance record = create().selectFrom(PROCESS_INSTANCE).where(PROCESS_INSTANCE.ID.eq(id)).fetchOne();

        if (record == null) {
            return null;
        }

        ProcessRecord result = new ProcessRecord();

        result.setId(record.getId());
        result.setStartTime(toTimestamp(record.getStartTime()));
        result.setEndTime(toTimestamp(record.getEndTime()));
        result.setProcessLog(new ProcessLog());
        result.setResult(EnumUtils.getEnum(ProcessResult.class, record.getResult()));
        result.setExitReason(EnumUtils.getEnum(ExitReason.class, record.getExitReason()));
        result.setPhase(EnumUtils.getEnum(ProcessPhase.class, record.getPhase()));
        result.setStartProcessServerId(record.getStartProcessServerId());
        result.setRunningProcessServerId(record.getRunningProcessServerId());
        result.setExecutionCount(record.getExecutionCount());
        result.setRunAfter(record.getRunAfter());

        result.setAccountId(record.getAccountId());
        result.setPriority(record.getPriority());
        result.setResourceType(record.getResourceType());
        result.setResourceId(record.getResourceId());
        result.setProcessName(record.getProcessName());
        result.setData(new HashMap<String, Object>(record.getData()));

        return result;
    }

    @Override
    public ProcessRecord insert(ProcessRecord record) {
        ProcessInstanceRecord pi = create().newRecord(PROCESS_INSTANCE);
        merge(pi, record);
        pi.insert();

        ProcessRecord newRecord = getRecord(pi.getId());
        newRecord.setPredicate(record.getPredicate());
        newRecord.setParentProcessState(record.getParentProcessState());

        return newRecord;
    }

    @Override
    public void update(ProcessRecord record, boolean schedule) {
        ProcessInstanceRecord pi = create()
                .selectFrom(PROCESS_INSTANCE)
                .where(PROCESS_INSTANCE.ID.eq(record.getId()))
                .fetchOne();

        if (pi == null) {
            throw new IllegalStateException("Failed to find process instance for [" + record.getId() + "]");
        }

        merge(pi, record);

        pi.update();

        if (schedule) {
            // For schedule we don't need to persist a processLog
            return;
        }

        ProcessLog processLog = record.getProcessLog();

        if (record.getId() != null && processLog != null && processLog.getUuid() != null) {
            String uuid = processLog.getUuid();
            Map<String, Object> log = convertToMap(record, processLog);

            int result = create()
                    .update(PROCESS_EXECUTION)
                        .set(PROCESS_EXECUTION.LOG, log)
                    .where(PROCESS_EXECUTION.UUID.eq(uuid))
                    .execute();

            if (result == 0) {
                create()
                    .insertInto(PROCESS_EXECUTION, PROCESS_EXECUTION.PROCESS_INSTANCE_ID, PROCESS_EXECUTION.UUID,
                            PROCESS_EXECUTION.LOG, PROCESS_EXECUTION.CREATED)
                    .values(record.getId(), uuid, log, new Timestamp(System.currentTimeMillis()))
                    .execute();
            }
        }
    }

    protected void merge(ProcessInstanceRecord pi, ProcessRecord record) {
        pi.setStartTime(toTimestamp(record.getStartTime()));
        pi.setEndTime(toTimestamp(record.getEndTime()));
        pi.setResult(ObjectUtils.toString(record.getResult(), null));
        pi.setExitReason(ObjectUtils.toString(record.getExitReason(), null));
        pi.setPhase(ObjectUtils.toString(record.getPhase(), null));
        pi.setStartProcessServerId(record.getStartProcessServerId());
        pi.setRunningProcessServerId(record.getRunningProcessServerId());
        pi.setExecutionCount(record.getExecutionCount());
        pi.setRunAfter(record.getRunAfter());

        if (record.getAccountId() instanceof Number) {
            pi.setAccountId(((Number)record.getAccountId()).longValue());
        }
        pi.setPriority(record.getPriority());
        pi.setResourceType(record.getResourceType());
        pi.setResourceId(record.getResourceId());
        pi.setProcessName(record.getProcessName());
        pi.setData(record.getData());
        pi.setPriority(record.getPriority());

        if (ExitReason.RETRY_EXCEPTION == record.getExitReason() || record.getRunAfter() == null) {
            pi.setRunAfter(new Date(System.currentTimeMillis()-300000));
        }
    }

    protected Timestamp toTimestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    protected <T> T convertToType(Object obj, Class<T> type) {
        return jsonMapper.convertValue(obj, type);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> convertToMap(ProcessRecord record, ProcessLog obj) {
        if (obj == null)
            return null;

        try {
            String stringData = jsonMapper.writeValueAsString(obj);
            if (stringData.length() > 1000000) {
                log.error("Process log is too long for id [{}] truncating executions : {}", record.getId(), stringData);
                obj.getExecutions().clear();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return jsonMapper.convertValue(obj, Map.class);
    }

    @Override
    public ProcessInstanceReference loadReference(Long id) {
        ProcessInstance record = objectManager.loadResource(ProcessInstance.class, id);
        if (record == null || record.getEndTime() != null) {
            return null;
        }

        ProcessInstanceReference ref = new ProcessInstanceReference();
        ref.setName(record.getProcessName());
        ref.setPriority(record.getPriority() == null ? 0 : record.getPriority());
        ref.setProcessId(record.getId());

        return ref;
    }

}
