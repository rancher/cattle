/*
 * This file is generated by jOOQ.
*/
package io.cattle.platform.core.model.tables;


import io.cattle.platform.core.model.CattleTable;
import io.cattle.platform.core.model.Keys;
import io.cattle.platform.core.model.tables.records.AgentRecord;
import io.cattle.platform.db.jooq.converter.DataConverter;
import io.cattle.platform.db.jooq.converter.DateConverter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AgentTable extends TableImpl<AgentRecord> {

    private static final long serialVersionUID = 956344574;

    /**
     * The reference instance of <code>cattle.agent</code>
     */
    public static final AgentTable AGENT = new AgentTable();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AgentRecord> getRecordType() {
        return AgentRecord.class;
    }

    /**
     * The column <code>cattle.agent.id</code>.
     */
    public final TableField<AgentRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>cattle.agent.name</code>.
     */
    public final TableField<AgentRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.agent.account_id</code>.
     */
    public final TableField<AgentRecord, Long> ACCOUNT_ID = createField("account_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>cattle.agent.kind</code>.
     */
    public final TableField<AgentRecord, String> KIND = createField("kind", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

    /**
     * The column <code>cattle.agent.uuid</code>.
     */
    public final TableField<AgentRecord, String> UUID = createField("uuid", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

    /**
     * The column <code>cattle.agent.description</code>.
     */
    public final TableField<AgentRecord, String> DESCRIPTION = createField("description", org.jooq.impl.SQLDataType.VARCHAR.length(1024), this, "");

    /**
     * The column <code>cattle.agent.state</code>.
     */
    public final TableField<AgentRecord, String> STATE = createField("state", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

    /**
     * The column <code>cattle.agent.created</code>.
     */
    public final TableField<AgentRecord, Date> CREATED = createField("created", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.agent.removed</code>.
     */
    public final TableField<AgentRecord, Date> REMOVED = createField("removed", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.agent.remove_time</code>.
     */
    public final TableField<AgentRecord, Date> REMOVE_TIME = createField("remove_time", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.agent.data</code>.
     */
    public final TableField<AgentRecord, Map<String,Object>> DATA = createField("data", org.jooq.impl.SQLDataType.CLOB, this, "", new DataConverter());

    /**
     * The column <code>cattle.agent.uri</code>.
     */
    public final TableField<AgentRecord, String> URI = createField("uri", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.agent.managed_config</code>.
     */
    public final TableField<AgentRecord, Boolean> MANAGED_CONFIG = createField("managed_config", org.jooq.impl.SQLDataType.BIT.nullable(false).defaultValue(org.jooq.impl.DSL.inline("b'1'", org.jooq.impl.SQLDataType.BIT)), this, "");

    /**
     * The column <code>cattle.agent.resource_account_id</code>.
     */
    public final TableField<AgentRecord, Long> RESOURCE_ACCOUNT_ID = createField("resource_account_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>cattle.agent.cluster_id</code>.
     */
    public final TableField<AgentRecord, Long> CLUSTER_ID = createField("cluster_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * Create a <code>cattle.agent</code> table reference
     */
    public AgentTable() {
        this("agent", null);
    }

    /**
     * Create an aliased <code>cattle.agent</code> table reference
     */
    public AgentTable(String alias) {
        this(alias, AGENT);
    }

    private AgentTable(String alias, Table<AgentRecord> aliased) {
        this(alias, aliased, null);
    }

    private AgentTable(String alias, Table<AgentRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return CattleTable.CATTLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<AgentRecord, Long> getIdentity() {
        return Keys.IDENTITY_AGENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<AgentRecord> getPrimaryKey() {
        return Keys.KEY_AGENT_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<AgentRecord>> getKeys() {
        return Arrays.<UniqueKey<AgentRecord>>asList(Keys.KEY_AGENT_PRIMARY, Keys.KEY_AGENT_IDX_AGENT_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ForeignKey<AgentRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<AgentRecord, ?>>asList(Keys.FK_AGENT__ACCOUNT_ID, Keys.FK_AGENT__RESOURCE_ACCOUNT_ID, Keys.FK_AGENT__CLUSTER_ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentTable as(String alias) {
        return new AgentTable(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AgentTable rename(String name) {
        return new AgentTable(name, null);
    }
}
