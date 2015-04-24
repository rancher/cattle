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
public class HostLabelMapTable extends org.jooq.impl.TableImpl<io.cattle.platform.core.model.tables.records.HostLabelMapRecord> {

	private static final long serialVersionUID = -1984088802;

	/**
	 * The singleton instance of <code>cattle.host_label_map</code>
	 */
	public static final io.cattle.platform.core.model.tables.HostLabelMapTable HOST_LABEL_MAP = new io.cattle.platform.core.model.tables.HostLabelMapTable();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<io.cattle.platform.core.model.tables.records.HostLabelMapRecord> getRecordType() {
		return io.cattle.platform.core.model.tables.records.HostLabelMapRecord.class;
	}

	/**
	 * The column <code>cattle.host_label_map.id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>cattle.host_label_map.name</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

	/**
	 * The column <code>cattle.host_label_map.account_id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.Long> ACCOUNT_ID = createField("account_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

	/**
	 * The column <code>cattle.host_label_map.kind</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.String> KIND = createField("kind", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

	/**
	 * The column <code>cattle.host_label_map.uuid</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.String> UUID = createField("uuid", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

	/**
	 * The column <code>cattle.host_label_map.description</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.String> DESCRIPTION = createField("description", org.jooq.impl.SQLDataType.VARCHAR.length(1024), this, "");

	/**
	 * The column <code>cattle.host_label_map.state</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.String> STATE = createField("state", org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

	/**
	 * The column <code>cattle.host_label_map.created</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.util.Date> CREATED = createField("created", org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

	/**
	 * The column <code>cattle.host_label_map.removed</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.util.Date> REMOVED = createField("removed", org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

	/**
	 * The column <code>cattle.host_label_map.remove_time</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.util.Date> REMOVE_TIME = createField("remove_time", org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

	/**
	 * The column <code>cattle.host_label_map.data</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.util.Map<String,Object>> DATA = createField("data", org.jooq.impl.SQLDataType.CLOB.length(65535).asConvertedDataType(new io.cattle.platform.db.jooq.converter.DataConverter()), this, "");

	/**
	 * The column <code>cattle.host_label_map.host_id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.Long> HOST_ID = createField("host_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

	/**
	 * The column <code>cattle.host_label_map.label_id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.Long> LABEL_ID = createField("label_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

	/**
	 * Create a <code>cattle.host_label_map</code> table reference
	 */
	public HostLabelMapTable() {
		this("host_label_map", null);
	}

	/**
	 * Create an aliased <code>cattle.host_label_map</code> table reference
	 */
	public HostLabelMapTable(java.lang.String alias) {
		this(alias, io.cattle.platform.core.model.tables.HostLabelMapTable.HOST_LABEL_MAP);
	}

	private HostLabelMapTable(java.lang.String alias, org.jooq.Table<io.cattle.platform.core.model.tables.records.HostLabelMapRecord> aliased) {
		this(alias, aliased, null);
	}

	private HostLabelMapTable(java.lang.String alias, org.jooq.Table<io.cattle.platform.core.model.tables.records.HostLabelMapRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, io.cattle.platform.core.model.CattleTable.CATTLE, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Identity<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, java.lang.Long> getIdentity() {
		return io.cattle.platform.core.model.Keys.IDENTITY_HOST_LABEL_MAP;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.HostLabelMapRecord> getPrimaryKey() {
		return io.cattle.platform.core.model.Keys.KEY_HOST_LABEL_MAP_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.HostLabelMapRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.HostLabelMapRecord>>asList(io.cattle.platform.core.model.Keys.KEY_HOST_LABEL_MAP_PRIMARY, io.cattle.platform.core.model.Keys.KEY_HOST_LABEL_MAP_IDX_HOST_LABEL_MAP_UUID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.ForeignKey<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, ?>> getReferences() {
		return java.util.Arrays.<org.jooq.ForeignKey<io.cattle.platform.core.model.tables.records.HostLabelMapRecord, ?>>asList(io.cattle.platform.core.model.Keys.FK_HOST_LABEL_MAP__ACCOUNT_ID, io.cattle.platform.core.model.Keys.FK_HOST_LABEL_MAP__HOST_ID, io.cattle.platform.core.model.Keys.FK_HOST_LABEL_MAP__LABEL_ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public io.cattle.platform.core.model.tables.HostLabelMapTable as(java.lang.String alias) {
		return new io.cattle.platform.core.model.tables.HostLabelMapTable(alias, this);
	}

	/**
	 * Rename this table
	 */
	public io.cattle.platform.core.model.tables.HostLabelMapTable rename(java.lang.String name) {
		return new io.cattle.platform.core.model.tables.HostLabelMapTable(name, null);
	}
}
