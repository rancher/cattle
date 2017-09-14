package io.cattle.platform.engine.manager.impl.jooq;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.tables.records.ProcessInstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.model.ProcessReference;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.log.ProcessLog;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.Record6;
import org.jooq.RecordHandler;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.cattle.platform.core.model.tables.ProcessExecutionTable.*;
import static io.cattle.platform.core.model.tables.ProcessInstanceTable.*;

public class JooqProcessRecordDao extends AbstractJooqDao implements ProcessRecordDao {

    private static final Logger log = LoggerFactory.getLogger(JooqProcessRecordDao.class);
    private static final DynamicIntProperty BATCH = ArchaiusUtil.getInt("process.replay.batch.size");
    private static final DynamicBooleanProperty SAVE_TERMINATING = ArchaiusUtil.getBoolean("process.save.terminating");

    JsonMapper jsonMapper;
    ObjectManager objectManager;
    ObjectMetaDataManager metaData;

    public JooqProcessRecordDao(Configuration configuration, JsonMapper jsonMapper, ObjectManager objectManager, ObjectMetaDataManager metaData) {
        super(configuration);
        this.jsonMapper = jsonMapper;
        this.objectManager = objectManager;
        this.metaData = metaData;
    }

    @Override
    public List<ProcessReference> pendingTasks() {
        final List<ProcessReference> result = new ArrayList<>();
        create()
            .select(PROCESS_INSTANCE.ID,
                    PROCESS_INSTANCE.PROCESS_NAME,
                    PROCESS_INSTANCE.RESOURCE_TYPE,
                    PROCESS_INSTANCE.RESOURCE_ID,
                    PROCESS_INSTANCE.ACCOUNT_ID,
                    PROCESS_INSTANCE.CLUSTER_ID)
            .from(PROCESS_INSTANCE)
                .where(PROCESS_INSTANCE.END_TIME.isNull()
                    .and(PROCESS_INSTANCE.RUN_AFTER.isNull().or(PROCESS_INSTANCE.RUN_AFTER.le(new Date()))))
                .limit(BATCH.get())
                .fetchInto((RecordHandler<Record6<Long, String, String, String, Long, Long>>) record -> {
                    ProcessReference ref = new ProcessReference(
                            record.getValue(PROCESS_INSTANCE.ID),
                            record.getValue(PROCESS_INSTANCE.PROCESS_NAME),
                            record.getValue(PROCESS_INSTANCE.RESOURCE_TYPE),
                            record.getValue(PROCESS_INSTANCE.RESOURCE_ID),
                            record.getValue(PROCESS_INSTANCE.ACCOUNT_ID),
                            record.getValue(PROCESS_INSTANCE.CLUSTER_ID));
                    result.add(ref);
                });

        return result;
    }

    @Override
    public ProcessRecord getRecord(Long id) {
        ProcessInstanceRecord record = create().selectFrom(PROCESS_INSTANCE).where(PROCESS_INSTANCE.ID.eq(id)).fetchOne();

        if (record == null) {
            return null;
        }

        return new JooqProcessRecord(record);
    }

    @Override
    public ProcessRecord insert(ProcessRecord record) {
        if (record.getRunAfter() == null) {
            record.setRunAfter(new Date(System.currentTimeMillis() + 60000 * 2));
        }
        ProcessInstanceRecord pi = create().newRecord(PROCESS_INSTANCE);
        merge(pi, record);
        pi.insert();

        JooqProcessRecord newRecord = new JooqProcessRecord(pi);
        newRecord.setParentProcessState(record.getParentProcessState());

        return newRecord;
    }

    @Override
    public void update(ProcessRecord record, boolean schedule) {
        if (!(record instanceof JooqProcessRecord)) {
            throw new IllegalStateException("Can not persist type [" + record.getClass() + "]");
        }

        ProcessInstanceRecord pi = ((JooqProcessRecord) record).getProcessInstance();

        pi.update();

        if (schedule) {
            // For schedule we don't need to persist a processLog
            return;
        }

        if (!SAVE_TERMINATING.get() && (record.getExitReason() == null ||
                record.getExitReason().isTerminating() || record.getExitReason() == ExitReason.WAITING)) {
            return;
        }

        ProcessLog processLog = record.getProcessLog();

        if (record.getId() != null && processLog != null && processLog.getUuid() != null) {
            String uuid = processLog.getUuid();
            create()
                .insertInto(PROCESS_EXECUTION,
                        PROCESS_EXECUTION.PROCESS_INSTANCE_ID,
                        PROCESS_EXECUTION.UUID,
                        PROCESS_EXECUTION.LOG,
                        PROCESS_EXECUTION.CREATED)
                .values(record.getId(),
                        uuid,
                        convertToMap(record, processLog),
                        new Timestamp(System.currentTimeMillis()))
                .execute();
        }
    }

    protected void merge(ProcessInstanceRecord pi, ProcessRecord record) {
        pi.setAccountId(record.getAccountId());
        pi.setClusterId(record.getClusterId());
        pi.setData(record.getData());
        pi.setEndTime(toTimestamp(record.getEndTime()));
        pi.setExecutionCount(record.getExecutionCount());
        pi.setExitReason(Objects.toString(record.getExitReason(), null));
        pi.setPriority(record.getPriority());
        pi.setPriority(record.getPriority());
        pi.setProcessName(record.getProcessName());
        pi.setResourceId(record.getResourceId());
        pi.setResourceType(record.getResourceType());
        pi.setResult(Objects.toString(record.getResult(), null));
        pi.setRunAfter(record.getRunAfter());
        pi.setRunningProcessServerId(record.getRunningProcessServerId());
        pi.setStartProcessServerId(record.getStartProcessServerId());
        pi.setStartTime(toTimestamp(record.getStartTime()));

        if (ExitReason.RETRY_EXCEPTION == record.getExitReason() || record.getRunAfter() == null) {
            pi.setRunAfter(new Date(System.currentTimeMillis()-300000));
        }
    }

    protected Timestamp toTimestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
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
    public void setDone(Object obj, String stateField, String state) {
        String type = objectManager.getType(obj);
        Class<?> clz = objectManager.getSchemaFactory().getSchemaClass(type);
        Table<?> table = JooqUtils.getTableFromRecordClass(clz);
        Field<Object> field = JooqUtils.getTableField(metaData, type, stateField);
        Field<Object> idField = JooqUtils.getTableField(metaData, type, ObjectMetaDataManager.ID_FIELD);

        create().update(table)
            .set(field, state)
            .where(idField.eq(io.cattle.platform.object.util.ObjectUtils.getId(obj)))
            .execute();
    }

}
