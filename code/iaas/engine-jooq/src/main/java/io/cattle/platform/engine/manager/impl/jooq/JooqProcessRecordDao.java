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
import org.jooq.Record3;
import org.jooq.RecordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;

public class JooqProcessRecordDao extends AbstractJooqDao implements ProcessRecordDao {

    private static DynamicIntProperty PROCESS_REPLAY_BATCH = ArchaiusUtil.getInt("process.replay.batch.size");

    private static final Logger log = LoggerFactory.getLogger(JooqProcessRecordDao.class);

    JsonMapper jsonMapper;

    @Override
    public List<Long> pendingTasks(String resourceType, String resourceId) {
        final List<Long> result = new ArrayList<Long>();
        /*
         * I know I should and can do this unique logic in SQL, but I couldn't
         * figure out a simple way to do it in HSQLDB. So if you're reading this
         * and can find a query that does this across all the supported DB,
         * please let someone know. Here's what I wanted to do select
         * min(PROCESS_INSTANCE.ID) from PROCESS_INSTANCE where
         * PROCESS_INSTANCE.END_TIME is null group by
         * PROCESS_INSTANCE.RESOURCE_TYPE, PROCESS_INSTANCE.RESOURCE_ID order by
         * PROCESS_INSTANCE.START_TIME asc limit 10000 offset 0 But you can't
         * order by something that is not in the group by. So how do I get a
         * unique pair of resource_type, resource_id, but still order by id or
         * start_time
         */
        final Set<String> seen = new HashSet<String>();
        create().select(PROCESS_INSTANCE.ID, PROCESS_INSTANCE.RESOURCE_TYPE, PROCESS_INSTANCE.RESOURCE_ID).from(PROCESS_INSTANCE).where(
                processCondition(resourceType, resourceId)).orderBy(PROCESS_INSTANCE.PRIORITY.desc(), PROCESS_INSTANCE.ID.asc()).limit(
                PROCESS_REPLAY_BATCH.get()).fetchInto(new RecordHandler<Record3<Long, String, String>>() {
            @Override
            public void next(Record3<Long, String, String> record) {
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
        ProcessInstanceRecord pi = create().selectFrom(PROCESS_INSTANCE).where(PROCESS_INSTANCE.ID.eq(record.getId())).fetchOne();

        if (pi == null) {
            throw new IllegalStateException("Failed to find process instance for [" + record.getId() + "]");
        }

        merge(pi, record);

        pi.update();

        /*
         * TODO: This is really a hack. For some reason if you persist the
         * process execution in schedule, the API thread will deadlock with a
         * process server thread in H2. Need to retest removing this. This may
         * have been fixed by some changes in the queries in the process server.
         */
        if (schedule) {
            return;
        }

        ProcessLog processLog = record.getProcessLog();

        if (record.getId() != null && processLog != null && processLog.getUuid() != null) {
            String uuid = processLog.getUuid();
            Map<String, Object> log = convertToMap(record, processLog);

            int result = create().update(PROCESS_EXECUTION).set(PROCESS_EXECUTION.LOG, log).where(PROCESS_EXECUTION.UUID.eq(uuid)).execute();

            if (result == 0) {
                create().insertInto(PROCESS_EXECUTION, PROCESS_EXECUTION.PROCESS_INSTANCE_ID, PROCESS_EXECUTION.UUID, PROCESS_EXECUTION.LOG).values(
                        record.getId(), uuid, log).execute();
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

        pi.setResourceType(record.getResourceType());
        pi.setResourceId(record.getResourceId());
        pi.setProcessName(record.getProcessName());
        pi.setData(record.getData());

        int priority = ArchaiusUtil.getInt("process." + record.getProcessName() + ".priority").get();
        pi.setPriority(priority);
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
