package io.cattle.platform.task.dao.impl;

import static io.cattle.platform.core.model.tables.TaskInstanceTable.*;
import static io.cattle.platform.core.model.tables.TaskTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Task;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.task.dao.TaskDao;
import java.util.Date;

import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;

public class TaskDaoImpl extends AbstractJooqDao implements TaskDao {

    private static Logger log = LoggerFactory.getLogger(TaskDaoImpl.class);

    private static DynamicLongProperty AFTER_SECONDS = ArchaiusUtil.getLong("task.purge.after.seconds");

    public void purgeOld() {
        int deleted = create()
            .delete(TASK_INSTANCE)
            .where(
                    TASK_INSTANCE.START_TIME.lt(new Date(System.currentTimeMillis() - AFTER_SECONDS.get() * 1000)))
            .execute();
        if ( deleted > 0 ) {
            log.info("Deleted [{}] task instance records", deleted);
        }
    }

    @Override
    public void register(String name) {
        Task task = getTask(name);
        if ( task == null ) {
            try {
                create()
                    .insertInto(TASK, TASK.NAME)
                    .values(name)
                    .execute();
            } catch ( DataAccessException e ) {
                if ( getTask(name) == null ) {
                    throw e;
                }
            }
        }
    }

    protected Task getTask(String name) {
        return create()
            .selectFrom(TASK)
            .where(TASK.NAME.eq(name))
            .fetchOne();
    }

}
