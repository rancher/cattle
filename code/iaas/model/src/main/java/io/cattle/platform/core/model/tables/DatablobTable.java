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
public class DatablobTable extends org.jooq.impl.TableImpl<io.cattle.platform.core.model.tables.records.DatablobRecord> {

	private static final long serialVersionUID = 1692064504;

	/**
	 * The singleton instance of <code>cattle.datablob</code>
	 */
	public static final io.cattle.platform.core.model.tables.DatablobTable DATABLOB = new io.cattle.platform.core.model.tables.DatablobTable();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<io.cattle.platform.core.model.tables.records.DatablobRecord> getRecordType() {
		return io.cattle.platform.core.model.tables.records.DatablobRecord.class;
	}

	/**
	 * The column <code>cattle.datablob.id</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.DatablobRecord, java.lang.Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>cattle.datablob.name</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.DatablobRecord, java.lang.String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

	/**
	 * The column <code>cattle.datablob.value</code>.
	 */
	public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.DatablobRecord, byte[]> VALUE = createField("value", org.jooq.impl.SQLDataType.BLOB.nullable(false), this, "");

	/**
	 * Create a <code>cattle.datablob</code> table reference
	 */
	public DatablobTable() {
		this("datablob", null);
	}

	/**
	 * Create an aliased <code>cattle.datablob</code> table reference
	 */
	public DatablobTable(java.lang.String alias) {
		this(alias, io.cattle.platform.core.model.tables.DatablobTable.DATABLOB);
	}

	private DatablobTable(java.lang.String alias, org.jooq.Table<io.cattle.platform.core.model.tables.records.DatablobRecord> aliased) {
		this(alias, aliased, null);
	}

	private DatablobTable(java.lang.String alias, org.jooq.Table<io.cattle.platform.core.model.tables.records.DatablobRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, io.cattle.platform.core.model.CattleTable.CATTLE, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Identity<io.cattle.platform.core.model.tables.records.DatablobRecord, java.lang.Long> getIdentity() {
		return io.cattle.platform.core.model.Keys.IDENTITY_DATABLOB;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.DatablobRecord> getPrimaryKey() {
		return io.cattle.platform.core.model.Keys.KEY_DATABLOB_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.DatablobRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.DatablobRecord>>asList(io.cattle.platform.core.model.Keys.KEY_DATABLOB_PRIMARY, io.cattle.platform.core.model.Keys.KEY_DATABLOB_IDX_DATA_NAME);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public io.cattle.platform.core.model.tables.DatablobTable as(java.lang.String alias) {
		return new io.cattle.platform.core.model.tables.DatablobTable(alias, this);
	}

	/**
	 * Rename this table
	 */
	public io.cattle.platform.core.model.tables.DatablobTable rename(java.lang.String name) {
		return new io.cattle.platform.core.model.tables.DatablobTable(name, null);
	}
}
