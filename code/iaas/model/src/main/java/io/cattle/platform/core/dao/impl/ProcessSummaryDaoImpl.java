package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.ProcessInstanceTable.*;

import io.cattle.platform.core.addon.ProcessSummary;
import io.cattle.platform.core.dao.ProcessSummaryDao;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Named;

import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.RecordHandler;
import org.jooq.impl.DSL;

@Named
public class ProcessSummaryDaoImpl extends AbstractJooqDao implements ProcessSummaryDao {

    @Override
    public List<ProcessSummary> getProcessSummary() {
        final Map<String, ProcessSummary> processSummary = new TreeMap<>();
        final Field<Boolean> running = DSL.field(PROCESS_INSTANCE.RUNNING_PROCESS_SERVER_ID.isNotNull()).as("running");
        final Field<Boolean> delayed = DSL.field(PROCESS_INSTANCE.RUN_AFTER.greaterThan(new Date())).as("foo");
        final Field<Integer> count = PROCESS_INSTANCE.PROCESS_NAME.count().as("count");

        create()
            .select(PROCESS_INSTANCE.PROCESS_NAME,
                    running,
                    delayed,
                    count)
            .from(PROCESS_INSTANCE)
            .where(PROCESS_INSTANCE.END_TIME.isNull())
            .groupBy(PROCESS_INSTANCE.PROCESS_NAME, running, delayed)
            .fetchInto(new RecordHandler<Record4<String, Boolean, Boolean, Integer>>() {
                @Override
                public void next(Record4<String, Boolean, Boolean, Integer> record) {
                    String name = record.getValue(PROCESS_INSTANCE.PROCESS_NAME);
                    int c = record.getValue(count);
                    boolean r = record.getValue(running);
                    Boolean d = record.getValue(delayed);
                    ProcessSummary summary = processSummary.get(name);
                    if (summary == null) {
                        summary = new ProcessSummary();
                        summary.setProcessName(name);
                        processSummary.put(name, summary);
                    }

                    if (r) {
                        summary.setRunning(summary.getRunning() + c);
                    } else if (d == null || !d) {
                        summary.setReady(summary.getReady() + c);
                    } else {
                        summary.setDelay(summary.getDelay() + c);
                    }
                }
            });

        return new ArrayList<>(processSummary.values());
    }

}
