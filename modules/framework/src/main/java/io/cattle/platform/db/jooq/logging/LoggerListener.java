package io.cattle.platform.db.jooq.logging;

import static org.jooq.conf.ParamType.*;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteType;
import org.jooq.tools.JooqLogger;
import org.jooq.tools.StringUtils;

public class LoggerListener extends org.jooq.tools.LoggerListener {

    private static final long serialVersionUID = 1991892878733157263L;

    private static final JooqLogger log = JooqLogger.getLogger(org.jooq.tools.LoggerListener.class);

    long maxLength = 1000;

    @Override
    public void renderEnd(ExecuteContext ctx) {
        if (log.isDebugEnabled()) {
            String[] batchSQL = ctx.batchSQL();

            if (ctx.query() != null) {

                // Actual SQL passed to JDBC
                log.debug("Executing query", ctx.sql());

                // [#1278] DEBUG log also SQL with inlined bind values, if
                // that is not the same as the actual SQL passed to JDBC
                String inlined = ctx.query().getSQL(INLINED);
                if (!ctx.sql().equals(inlined)) {
                    if (inlined.length() > maxLength) {
                        log.debug("-> with bind values : too long");
                    } else {
                        log.debug("-> with bind values", inlined);
                    }
                }
            } else if (!StringUtils.isBlank(ctx.sql())) {

                // [#1529] Batch queries should be logged specially
                if (ctx.type() == ExecuteType.BATCH) {
                    log.debug("Executing batch query", ctx.sql());
                } else {
                    log.debug("Executing query", ctx.sql());
                }
            }

            // [#2532] Log a complete BatchMultiple query
            else if (batchSQL.length > 0) {
                if (batchSQL[batchSQL.length - 1] != null) {
                    for (String sql : batchSQL) {
                        log.debug("Executing batch query", sql);
                    }
                }
            }
        }
    }

    public long getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(long maxLength) {
        this.maxLength = maxLength;
    }

}
