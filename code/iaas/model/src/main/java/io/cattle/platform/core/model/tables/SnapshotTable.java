/**
 * This class is generated by jOOQ
 */
package io.cattle.platform.core.model.tables;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = { "http://www.jooq.org", "3.3.0" },
                            comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SnapshotTable extends org.jooq.impl.TableImpl<io.cattle.platform.core.model.tables.records.SnapshotRecord> {

	private static final long serialVersionUID = -178037970;

	/**
	 * The singleton instance of <code>cattle.snapshot</code>
	 */
	public static final io.cattle.platform.core.model.tables.SnapshotTable SNAPSHOT = new io.cattle.platform.core.model.tables.SnapshotTable();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<io.cattle.platform.core.model.tables.records.SnapshotRecord> getRecordType() {
		return io.cattle.platform.core.model.tables.records.SnapshotRecord.class;
	}

	/**
	 * The column <code>cattle.snapshot.id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>cattle.snapshot.name</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

	/**
	 * The column <code>cattle.snapshot.account_id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.Long> ACCOUNT_ID = createField("account_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

	/**
	 * The column <code>cattle.snapshot.kind</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.String> KIND = createField("kind", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

	/**
	 * The column <code>cattle.snapshot.uuid</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.String> UUID = createField("uuid", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

	/**
	 * The column <code>cattle.snapshot.description</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.String> DESCRIPTION = createField("description", org.jooq.impl.SQLDataType.VARCHAR.length(1024), this, "");

	/**
	 * The column <code>cattle.snapshot.state</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.String> STATE = createField("state", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

	/**
	 * The column <code>cattle.snapshot.created</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.util.Date> CREATED = createField("created", org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

	/**
	 * The column <code>cattle.snapshot.removed</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.util.Date> REMOVED = createField("removed", org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

	/**
	 * The column <code>cattle.snapshot.remove_time</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.util.Date> REMOVE_TIME = createField("remove_time", org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

	/**
	 * The column <code>cattle.snapshot.data</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.util.Map<String,Object>> DATA = createField("data", org.jooq.impl.SQLDataType.CLOB.length(16777215).asConvertedDataType(new io.cattle.platform.db.jooq.converter.DataConverter()), this, "");

	/**
	 * The column <code>cattle.snapshot.volume_id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.Long> VOLUME_ID = createField("volume_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

	/**
	 * The column <code>cattle.snapshot.backup_target_id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.Long> BACKUP_TARGET_ID = createField("backup_target_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

	/**
	 * The column <code>cattle.snapshot.backup_uri</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.String> BACKUP_URI = createField("backup_uri", org.jooq.impl.SQLDataType.VARCHAR.length(4096), this, "");

	/**
	 * Create a <code>cattle.snapshot</code> table reference
	 */
	public SnapshotTable() {
		this("snapshot", null);
	}

	/**
	 * Create an aliased <code>cattle.snapshot</code> table reference
	 */
	public SnapshotTable(java.lang.String alias) {
		this(alias, io.cattle.platform.core.model.tables.SnapshotTable.SNAPSHOT);
	}

	private SnapshotTable(java.lang.String alias, org.jooq.Table<io.cattle.platform.core.model.tables.records.SnapshotRecord> aliased) {
		this(alias, aliased, null);
	}

	private SnapshotTable(java.lang.String alias, org.jooq.Table<io.cattle.platform.core.model.tables.records.SnapshotRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, io.cattle.platform.core.model.CattleTable.CATTLE, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Identity<io.cattle.platform.core.model.tables.records.SnapshotRecord, java.lang.Long> getIdentity() {
		return io.cattle.platform.core.model.Keys.IDENTITY_SNAPSHOT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.SnapshotRecord> getPrimaryKey() {
		return io.cattle.platform.core.model.Keys.KEY_SNAPSHOT_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.SnapshotRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.SnapshotRecord>>asList(io.cattle.platform.core.model.Keys.KEY_SNAPSHOT_PRIMARY, io.cattle.platform.core.model.Keys.KEY_SNAPSHOT_IDX_SNAPSHOT_UUID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.ForeignKey<io.cattle.platform.core.model.tables.records.SnapshotRecord, ?>> getReferences() {
		return java.util.Arrays.<org.jooq.ForeignKey<io.cattle.platform.core.model.tables.records.SnapshotRecord, ?>>asList(io.cattle.platform.core.model.Keys.FK_SNAPSHOT__ACCOUNT_ID, io.cattle.platform.core.model.Keys.FK_SNAPSHOT__VOLUME_ID, io.cattle.platform.core.model.Keys.FK_SNAPSHOT__BACKUP_TARGET_ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public io.cattle.platform.core.model.tables.SnapshotTable as(java.lang.String alias) {
		return new io.cattle.platform.core.model.tables.SnapshotTable(alias, this);
	}

	/**
	 * Rename this table
	 */
	public io.cattle.platform.core.model.tables.SnapshotTable rename(java.lang.String name) {
		return new io.cattle.platform.core.model.tables.SnapshotTable(name, null);
	}
}
