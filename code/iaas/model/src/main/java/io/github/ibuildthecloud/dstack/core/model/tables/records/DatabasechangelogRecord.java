/**
 * This class is generated by jOOQ
 */
package io.github.ibuildthecloud.dstack.core.model.tables.records;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = { "http://www.jooq.org", "3.3.0" },
                            comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
@javax.persistence.Entity
@javax.persistence.Table(name = "DATABASECHANGELOG", schema = "dstack")
public class DatabasechangelogRecord extends org.jooq.impl.TableRecordImpl<io.github.ibuildthecloud.dstack.core.model.tables.records.DatabasechangelogRecord> implements io.github.ibuildthecloud.dstack.db.jooq.utils.TableRecordJaxb, org.jooq.Record11<java.lang.String, java.lang.String, java.lang.String, java.util.Date, java.lang.Integer, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String>, io.github.ibuildthecloud.dstack.core.model.Databasechangelog {

	private static final long serialVersionUID = 631484558;

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.ID</code>.
	 */
	@Override
	public void setId(java.lang.String value) {
		setValue(0, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.ID</code>.
	 */
	@javax.persistence.Column(name = "ID", nullable = false, length = 255)
	@Override
	public java.lang.String getId() {
		return (java.lang.String) getValue(0);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.AUTHOR</code>.
	 */
	@Override
	public void setAuthor(java.lang.String value) {
		setValue(1, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.AUTHOR</code>.
	 */
	@javax.persistence.Column(name = "AUTHOR", nullable = false, length = 255)
	@Override
	public java.lang.String getAuthor() {
		return (java.lang.String) getValue(1);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.FILENAME</code>.
	 */
	@Override
	public void setFilename(java.lang.String value) {
		setValue(2, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.FILENAME</code>.
	 */
	@javax.persistence.Column(name = "FILENAME", nullable = false, length = 255)
	@Override
	public java.lang.String getFilename() {
		return (java.lang.String) getValue(2);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.DATEEXECUTED</code>.
	 */
	@Override
	public void setDateexecuted(java.util.Date value) {
		setValue(3, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.DATEEXECUTED</code>.
	 */
	@javax.persistence.Column(name = "DATEEXECUTED", nullable = false)
	@Override
	public java.util.Date getDateexecuted() {
		return (java.util.Date) getValue(3);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.ORDEREXECUTED</code>.
	 */
	@Override
	public void setOrderexecuted(java.lang.Integer value) {
		setValue(4, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.ORDEREXECUTED</code>.
	 */
	@javax.persistence.Column(name = "ORDEREXECUTED", nullable = false, precision = 10)
	@Override
	public java.lang.Integer getOrderexecuted() {
		return (java.lang.Integer) getValue(4);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.EXECTYPE</code>.
	 */
	@Override
	public void setExectype(java.lang.String value) {
		setValue(5, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.EXECTYPE</code>.
	 */
	@javax.persistence.Column(name = "EXECTYPE", nullable = false, length = 10)
	@Override
	public java.lang.String getExectype() {
		return (java.lang.String) getValue(5);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.MD5SUM</code>.
	 */
	@Override
	public void setMd5sum(java.lang.String value) {
		setValue(6, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.MD5SUM</code>.
	 */
	@javax.persistence.Column(name = "MD5SUM", length = 35)
	@Override
	public java.lang.String getMd5sum() {
		return (java.lang.String) getValue(6);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.DESCRIPTION</code>.
	 */
	@Override
	public void setDescription(java.lang.String value) {
		setValue(7, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.DESCRIPTION</code>.
	 */
	@javax.persistence.Column(name = "DESCRIPTION", length = 255)
	@Override
	public java.lang.String getDescription() {
		return (java.lang.String) getValue(7);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.COMMENTS</code>.
	 */
	@Override
	public void setComments(java.lang.String value) {
		setValue(8, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.COMMENTS</code>.
	 */
	@javax.persistence.Column(name = "COMMENTS", length = 255)
	@Override
	public java.lang.String getComments() {
		return (java.lang.String) getValue(8);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.TAG</code>.
	 */
	@Override
	public void setTag(java.lang.String value) {
		setValue(9, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.TAG</code>.
	 */
	@javax.persistence.Column(name = "TAG", length = 255)
	@Override
	public java.lang.String getTag() {
		return (java.lang.String) getValue(9);
	}

	/**
	 * Setter for <code>dstack.DATABASECHANGELOG.LIQUIBASE</code>.
	 */
	@Override
	public void setLiquibase(java.lang.String value) {
		setValue(10, value);
	}

	/**
	 * Getter for <code>dstack.DATABASECHANGELOG.LIQUIBASE</code>.
	 */
	@javax.persistence.Column(name = "LIQUIBASE", length = 20)
	@Override
	public java.lang.String getLiquibase() {
		return (java.lang.String) getValue(10);
	}

	// -------------------------------------------------------------------------
	// Record11 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row11<java.lang.String, java.lang.String, java.lang.String, java.util.Date, java.lang.Integer, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String> fieldsRow() {
		return (org.jooq.Row11) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row11<java.lang.String, java.lang.String, java.lang.String, java.util.Date, java.lang.Integer, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String> valuesRow() {
		return (org.jooq.Row11) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field1() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field2() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.AUTHOR;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field3() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.FILENAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.util.Date> field4() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.DATEEXECUTED;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field5() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.ORDEREXECUTED;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field6() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.EXECTYPE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field7() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.MD5SUM;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field8() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.DESCRIPTION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field9() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.COMMENTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field10() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.TAG;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field11() {
		return io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG.LIQUIBASE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value1() {
		return getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value2() {
		return getAuthor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value3() {
		return getFilename();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.Date value4() {
		return getDateexecuted();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value5() {
		return getOrderexecuted();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value6() {
		return getExectype();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value7() {
		return getMd5sum();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value8() {
		return getDescription();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value9() {
		return getComments();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value10() {
		return getTag();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value11() {
		return getLiquibase();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value1(java.lang.String value) {
		setId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value2(java.lang.String value) {
		setAuthor(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value3(java.lang.String value) {
		setFilename(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value4(java.util.Date value) {
		setDateexecuted(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value5(java.lang.Integer value) {
		setOrderexecuted(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value6(java.lang.String value) {
		setExectype(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value7(java.lang.String value) {
		setMd5sum(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value8(java.lang.String value) {
		setDescription(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value9(java.lang.String value) {
		setComments(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value10(java.lang.String value) {
		setTag(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord value11(java.lang.String value) {
		setLiquibase(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DatabasechangelogRecord values(java.lang.String value1, java.lang.String value2, java.lang.String value3, java.util.Date value4, java.lang.Integer value5, java.lang.String value6, java.lang.String value7, java.lang.String value8, java.lang.String value9, java.lang.String value10, java.lang.String value11) {
		return this;
	}

	// -------------------------------------------------------------------------
	// FROM and INTO
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void from(io.github.ibuildthecloud.dstack.core.model.Databasechangelog from) {
		setId(from.getId());
		setAuthor(from.getAuthor());
		setFilename(from.getFilename());
		setDateexecuted(from.getDateexecuted());
		setOrderexecuted(from.getOrderexecuted());
		setExectype(from.getExectype());
		setMd5sum(from.getMd5sum());
		setDescription(from.getDescription());
		setComments(from.getComments());
		setTag(from.getTag());
		setLiquibase(from.getLiquibase());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends io.github.ibuildthecloud.dstack.core.model.Databasechangelog> E into(E into) {
		into.from(this);
		return into;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached DatabasechangelogRecord
	 */
	public DatabasechangelogRecord() {
		super(io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG);
	}

	/**
	 * Create a detached, initialised DatabasechangelogRecord
	 */
	public DatabasechangelogRecord(java.lang.String id, java.lang.String author, java.lang.String filename, java.util.Date dateexecuted, java.lang.Integer orderexecuted, java.lang.String exectype, java.lang.String md5sum, java.lang.String description, java.lang.String comments, java.lang.String tag, java.lang.String liquibase) {
		super(io.github.ibuildthecloud.dstack.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG);

		setValue(0, id);
		setValue(1, author);
		setValue(2, filename);
		setValue(3, dateexecuted);
		setValue(4, orderexecuted);
		setValue(5, exectype);
		setValue(6, md5sum);
		setValue(7, description);
		setValue(8, comments);
		setValue(9, tag);
		setValue(10, liquibase);
	}
}
