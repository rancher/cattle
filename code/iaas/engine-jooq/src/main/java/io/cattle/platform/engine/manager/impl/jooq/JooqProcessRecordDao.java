package io.cattle.platform.engine.manager.impl.jooq;

import static io.cattle.platform.core.model.tables.ProcessExecutionTable.*;
import static io.cattle.platform.core.model.tables.ProcessInstanceTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.tables.records.ProcessInstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessPhase;
import io.cattle.platform.engine.process.ProcessResult;
import io.cattle.platform.engine.process.log.ProcessLog;
import io.cattle.platform.json.JsonMapper;

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
import org.jooq.Record4;
import org.jooq.RecordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;

public class JooqProcessRecordDao extends AbstractJooqDao implements ProcessRecordDao {

    private static final Logger log = LoggerFactory.getLogger(JooqProcessRecordDao.class);
    private static final DynamicIntProperty BATCH = ArchaiusUtil.getInt("process.replay.batch.size");

    JsonMapper jsonMapper;

    @Override
    public List<Long> pendingTasks(String resourceType, String resourceId, final boolean priority) {
        final List<Long> result = new ArrayList<Long>();
        final Set<String> seen = new HashSet<String>();
        create()
            .select(PROCESS_INSTANCE.ID, PROCESS_INSTANCE.RESOURCE_TYPE, PROCESS_INSTANCE.RESOURCE_ID,
                    PROCESS_INSTANCE.PRIORITY)
            .from(PROCESS_INSTANCE)
                .where(processCondition(resourceType, resourceId))
                    .and(PROCESS_INSTANCE.RUN_AFTER.isNull()
                            .or(PROCESS_INSTANCE.RUN_AFTER.le(new Date())))
                .orderBy(PROCESS_INSTANCE.ID.asc(),
                        PROCESS_INSTANCE.PRIORITY.desc())
                .limit(BATCH.get())
                .fetchInto(new RecordHandler<Record4<Long, String, String, Integer>>() {
            @Override
            public void next(Record4<Long, String, String, Integer> record) {
                if (priority && (record.value4() == null || record.value4() <= 0L)) {
                    return;
                }
                String resource = String.format("%s:%s", record.value2(), record.value3());
                if (seen.contains(resource)) {
                    return;
                }

                seen.add(resource);
                result.add(record.value1());
            }
        });

        return result;
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

        pi.setResourceType(record.getResourceType());
        pi.setResourceId(record.getResourceId());
        pi.setProcessName(record.getProcessName());
        pi.setData(record.getData());

        int priority = ArchaiusUtil.getInt("process." + record.getProcessName() + ".priority").get();
        pi.setPriority(priority);

        if (ExitReason.RETRY_EXCEPTION == record.getExitReason()) {
            pi.setRunAfter(null);
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

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}
