/*
 * This file is generated by jOOQ.
*/
package io.cattle.platform.core.model.tables;


import io.cattle.platform.core.model.CattleTable;
import io.cattle.platform.core.model.Keys;
import io.cattle.platform.core.model.tables.records.AccountRecord;
import io.cattle.platform.db.jooq.converter.DataConverter;
import io.cattle.platform.db.jooq.converter.DateConverter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Generated;

import org.jooq.Field;
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
public class AccountTable extends TableImpl<AccountRecord> {

    private static final long serialVersionUID = -1921812411;

    /**
     * The reference instance of <code>cattle.account</code>
     */
    public static final AccountTable ACCOUNT = new AccountTable();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AccountRecord> getRecordType() {
        return AccountRecord.class;
    }

    /**
     * The column <code>cattle.account.id</code>.
     */
    public final TableField<AccountRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>cattle.account.name</code>.
     */
    public final TableField<AccountRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.account.kind</code>.
     */
    public final TableField<AccountRecord, String> KIND = createField("kind", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

    /**
     * The column <code>cattle.account.uuid</code>.
     */
    public final TableField<AccountRecord, String> UUID = createField("uuid", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

    /**
     * The column <code>cattle.account.description</code>.
     */
    public final TableField<AccountRecord, String> DESCRIPTION = createField("description", org.jooq.impl.SQLDataType.VARCHAR.length(1024), this, "");

    /**
     * The column <code>cattle.account.state</code>.
     */
    public final TableField<AccountRecord, String> STATE = createField("state", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

    /**
     * The column <code>cattle.account.created</code>.
     */
    public final TableField<AccountRecord, Date> CREATED = createField("created", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.account.removed</code>.
     */
    public final TableField<AccountRecord, Date> REMOVED = createField("removed", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.account.remove_time</code>.
     */
    public final TableField<AccountRecord, Date> REMOVE_TIME = createField("remove_time", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new DateConverter());

    /**
     * The column <code>cattle.account.data</code>.
     */
    public final TableField<AccountRecord, Map<String,Object>> DATA = createField("data", org.jooq.impl.SQLDataType.CLOB, this, "", new DataConverter());

    /**
     * The column <code>cattle.account.external_id</code>.
     */
    public final TableField<AccountRecord, String> EXTERNAL_ID = createField("external_id", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.account.external_id_type</code>.
     */
    public final TableField<AccountRecord, String> EXTERNAL_ID_TYPE = createField("external_id_type", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

    /**
     * The column <code>cattle.account.default_network_id</code>.
     */
    public final TableField<AccountRecord, Long> DEFAULT_NETWORK_ID = createField("default_network_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * Create a <code>cattle.account</code> table reference
     */
    public AccountTable() {
        this("account", null);
    }

    /**
     * Create an aliased <code>cattle.account</code> table reference
     */
    public AccountTable(String alias) {
        this(alias, ACCOUNT);
    }

    private AccountTable(String alias, Table<AccountRecord> aliased) {
        this(alias, aliased, null);
    }

    private AccountTable(String alias, Table<AccountRecord> aliased, Field<?>[] parameters) {
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
    public Identity<AccountRecord, Long> getIdentity() {
        return Keys.IDENTITY_ACCOUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<AccountRecord> getPrimaryKey() {
        return Keys.KEY_ACCOUNT_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<AccountRecord>> getKeys() {
        return Arrays.<UniqueKey<AccountRecord>>asList(Keys.KEY_ACCOUNT_PRIMARY, Keys.KEY_ACCOUNT_IDX_ACCOUNT_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountTable as(String alias) {
        return new AccountTable(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AccountTable rename(String name) {
        return new AccountTable(name, null);
    }
}
