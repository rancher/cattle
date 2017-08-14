/*
 * This file is generated by jOOQ.
*/
package io.cattle.platform.core.model.tables;


import io.cattle.platform.core.model.CattleTable;
import io.cattle.platform.core.model.Keys;
import io.cattle.platform.core.model.tables.records.RevisionRecord;
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
public class RevisionTable extends TableImpl<RevisionRecord> {

    private static final long serialVersionUID = 819842493;

    /**
     * The reference instance of <code>cattle.revision</code>
     */
    public static final RevisionTable REVISION = new RevisionTable();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<RevisionRecord> getRecordType() {
        return RevisionRecord.class;
    }

    /**
     * The column <code>cattle.revision.id</code>.
     */
    public final TableField<RevisionRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>cattle.revision.name</code>.
     */
    public final TableField<RevisionRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.revision.account_id</code>.
     */
    public final TableField<RevisionRecord, Long> ACCOUNT_ID = createField("account_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>cattle.revision.kind</code>.
     */
    public final TableField<RevisionRecord, String> KIND = createField("kind", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

    /**
     * The column <code>cattle.revision.uuid</code>.
     */
    public final TableField<RevisionRecord, String> UUID = createField("uuid", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

    /**
     * The column <code>cattle.revision.description</code>.
     */
    public final TableField<RevisionRecord, String> DESCRIPTION = createField("description", org.jooq.impl.SQLDataType.VARCHAR.length(1024), this, "");

    /**
     * The column <code>cattle.revision.state</code>.
     */
    public final TableField<RevisionRecord, String> STATE = createField("state", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

    /**
     * The column <code>cattle.revision.created</code>.
     */
    public final TableField<RevisionRecord, Date> CREATED = createField("created", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.revision.removed</code>.
     */
    public final TableField<RevisionRecord, Date> REMOVED = createField("removed", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.revision.remove_time</code>.
     */
    public final TableField<RevisionRecord, Date> REMOVE_TIME = createField("remove_time", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.revision.data</code>.
     */
    public final TableField<RevisionRecord, Map<String,Object>> DATA = createField("data", org.jooq.impl.SQLDataType.CLOB, this, "", new DataConverter());

    /**
     * The column <code>cattle.revision.service_id</code>.
     */
    public final TableField<RevisionRecord, Long> SERVICE_ID = createField("service_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>cattle.revision.creator_id</code>.
     */
    public final TableField<RevisionRecord, Long> CREATOR_ID = createField("creator_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * Create a <code>cattle.revision</code> table reference
     */
    public RevisionTable() {
        this("revision", null);
    }

    /**
     * Create an aliased <code>cattle.revision</code> table reference
     */
    public RevisionTable(String alias) {
        this(alias, REVISION);
    }

    private RevisionTable(String alias, Table<RevisionRecord> aliased) {
        this(alias, aliased, null);
    }

    private RevisionTable(String alias, Table<RevisionRecord> aliased, Field<?>[] parameters) {
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
    public Identity<RevisionRecord, Long> getIdentity() {
        return Keys.IDENTITY_REVISION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<RevisionRecord> getPrimaryKey() {
        return Keys.KEY_REVISION_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<RevisionRecord>> getKeys() {
        return Arrays.<UniqueKey<RevisionRecord>>asList(Keys.KEY_REVISION_PRIMARY, Keys.KEY_REVISION_IDX_REVISION_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ForeignKey<RevisionRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<RevisionRecord, ?>>asList(Keys.FK_REVISION__ACCOUNT_ID, Keys.FK_REVISION__SERVICE_ID, Keys.FK_REVISION__CREATOR_ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RevisionTable as(String alias) {
        return new RevisionTable(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public RevisionTable rename(String name) {
        return new RevisionTable(name, null);
    }
}
