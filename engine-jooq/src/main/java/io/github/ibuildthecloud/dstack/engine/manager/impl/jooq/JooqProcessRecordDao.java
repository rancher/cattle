package io.github.ibuildthecloud.dstack.engine.manager.impl.jooq;

import static io.github.ibuildthecloud.dstack.core.tables.ProcessInstanceTable.*;
import io.github.ibuildthecloud.dstack.core.tables.records.ProcessInstanceRecord;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.engine.manager.impl.ProcessRecord;
import io.github.ibuildthecloud.dstack.engine.manager.impl.ProcessRecordDao;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessPhase;
import io.github.ibuildthecloud.dstack.engine.process.ProcessResult;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;
import io.github.ibuildthecloud.dstack.json.JsonMapper;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JooqProcessRecordDao extends AbstractJooqDao implements ProcessRecordDao {

    private static final Logger log = LoggerFactory.getLogger(JooqProcessRecordDao.class);

    JsonMapper jsonMapper;

    @Override
    public List<Long> pendingTasks() {
        return create()
                .select(PROCESS_INSTANCE.ID)
                .from(PROCESS_INSTANCE)
                .where(PROCESS_INSTANCE.END_TIME.isNull())
                .orderBy(PROCESS_INSTANCE.START_TIME.asc())
                .fetch(PROCESS_INSTANCE.ID);
    }

    @Override
    public ProcessRecord getRecord(Long id) {
        io.github.ibuildthecloud.dstack.core.model.ProcessInstance record = create()
                .selectFrom(PROCESS_INSTANCE)
                .where(PROCESS_INSTANCE.ID.eq(id))
                .fetchOne();

        ProcessRecord result = new ProcessRecord();

        result.setId(record.getId());
        result.setStartTime(toTimestamp(record.getStartTime()));
        result.setEndTime(toTimestamp(record.getEndTime()));
        result.setProcessLog(convertToType(record.getLog(), ProcessLog.class));
        result.setResult(EnumUtils.getEnum(ProcessResult.class, record.getResult()));
        result.setExitReason(EnumUtils.getEnum(ExitReason.class, record.getExitReason()));
        result.setPhase(EnumUtils.getEnum(ProcessPhase.class, record.getPhase()));
        result.setStartProcessServerId(record.getStartProcessServerId());
        result.setRunningProcessServerId(record.getRunningProcessServerId());

        result.setResourceType(record.getResourceType());
        result.setResourceId(record.getResourceId());
        result.setProcessName(record.getProcessName());
        result.setData(record.getData());

        return result;
    }

    @Override
    public ProcessRecord insert(ProcessRecord record) {
        ProcessInstanceRecord pi = create().newRecord(PROCESS_INSTANCE);
        merge(pi, record);
        pi.insert();

        return getRecord(pi.getId());
    }

    @Override
    public void update(ProcessRecord record) {
        ProcessInstanceRecord pi =
                create().selectFrom(PROCESS_INSTANCE)
                        .where(PROCESS_INSTANCE.ID.eq(record.getId()))
                        .fetchOne();

        if ( pi == null ) {
            throw new IllegalStateException("Failed to find process instance for [" + record.getId() + "]");
        }

        merge(pi, record);

        pi.update();
    }

    protected void merge(ProcessInstanceRecord pi, ProcessRecord record) {
        pi.setStartTime(toTimestamp(record.getStartTime()));
        pi.setEndTime(toTimestamp(record.getEndTime()));
        pi.setLog(convertToMap(record, record.getProcessLog()));
        pi.setResult(ObjectUtils.toString(record.getResult(), null));
        pi.setExitReason(ObjectUtils.toString(record.getExitReason(), null));
        pi.setPhase(ObjectUtils.toString(record.getPhase(), null));
        pi.setStartProcessServerId(record.getStartProcessServerId());
        pi.setRunningProcessServerId(record.getRunningProcessServerId());

        pi.setResourceType(record.getResourceType());
        pi.setResourceId(record.getResourceId());
        pi.setProcessName(record.getProcessName());
        pi.setData(record.getData());
    }

    protected Timestamp toTimestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    protected <T> T convertToType(Object obj, Class<T> type) {
        return jsonMapper.convertValue(obj, type);
    }

    @SuppressWarnings("unchecked")
    protected Map<String,Object> convertToMap(ProcessRecord record, ProcessLog obj) {
        if ( obj == null )
            return null;

        try {
            String stringData = jsonMapper.writeValueAsString(obj);
            if ( stringData.length() > 1000000 ) {
                log.error("Process log is too long for id [{}] truncating executions : {}", record.getId(), null);
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
