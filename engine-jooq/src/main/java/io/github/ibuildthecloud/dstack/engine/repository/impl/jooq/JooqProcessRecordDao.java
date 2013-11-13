package io.github.ibuildthecloud.dstack.engine.repository.impl.jooq;

import static io.github.ibuildthecloud.dstack.db.jooq.generated.tables.ProcessInstance.*;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.db.jooq.generated.tables.records.ProcessInstanceRecord;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessPhase;
import io.github.ibuildthecloud.dstack.engine.process.ProcessResult;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;
import io.github.ibuildthecloud.dstack.engine.repository.impl.ProcessRecord;
import io.github.ibuildthecloud.dstack.engine.repository.impl.ProcessRecordDao;
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

    @SuppressWarnings("unchecked")
    @Override
    public ProcessRecord getRecord(Long id) {
        ProcessInstanceRecord record = create()
                .selectFrom(PROCESS_INSTANCE)
                .where(PROCESS_INSTANCE.ID.eq(id))
                .fetchOne();

        ProcessRecord result = new ProcessRecord();

        result.setId(record.getId());
        result.setStartTime(toTimestamp(record.getStartTime()));
        result.setEndTime(toTimestamp(record.getEndTime()));
        result.setProcessLog(jsonToObj(record.getLog(), ProcessLog.class));
        result.setResult(EnumUtils.getEnum(ProcessResult.class, record.getResult()));
        result.setExitReason(EnumUtils.getEnum(ExitReason.class, record.getExitReason()));
        result.setPhase(EnumUtils.getEnum(ProcessPhase.class, record.getPhase()));
        result.setStartProcessServerId(record.getStartProcessServerId());
        result.setRunningProcessServerId(record.getRunningProcessServerId());

        result.setResourceType(record.getResourceType());
        result.setResourceId(record.getResourceId());
        result.setProcessName(record.getProcessName());
        result.setData(jsonToObj(record.getData(), Map.class));

        return result;
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

        pi.setStartTime(toTimestamp(record.getStartTime()));
        pi.setEndTime(toTimestamp(record.getEndTime()));
        pi.setLog(objToJson(record.getProcessLog()));
        pi.setResult(ObjectUtils.toString(record.getResult()));
        pi.setExitReason(ObjectUtils.toString(record.getExitReason()));
        pi.setPhase(ObjectUtils.toString(record.getPhase()));
        pi.setStartProcessServerId(record.getStartProcessServerId());
        pi.setRunningProcessServerId(record.getRunningProcessServerId());

        pi.setResourceType(record.getResourceType());
        pi.setResourceId(record.getResourceId());
        pi.setProcessName(record.getProcessName());
        pi.setData(objToJson(record.getData()));

        pi.update();
    }

    protected Timestamp toTimestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    protected <T> T jsonToObj(String string, Class<T> type) {
        try {
            return jsonMapper.readValue(string, type);
        } catch (IOException e) {
            log.error("Failed to unmarshall json for [{}]", string, e);
            throw new IllegalStateException(e);
        }
    }

    protected String objToJson(Object obj) {
        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (IOException e) {
            log.error("Failed to marshall json for [{}]", obj, e);
            return "{}";
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}
