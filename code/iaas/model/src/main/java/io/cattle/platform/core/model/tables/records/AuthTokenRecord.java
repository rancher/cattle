/**
 * This class is generated by jOOQ
 */
package io.cattle.platform.core.model.tables.records;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = { "http://www.jooq.org", "3.3.0" },
                            comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
@javax.persistence.Entity
@javax.persistence.Table(name = "auth_token", schema = "cattle")
public class AuthTokenRecord extends org.jooq.impl.UpdatableRecordImpl<io.cattle.platform.core.model.tables.records.AuthTokenRecord> implements io.cattle.platform.db.jooq.utils.TableRecordJaxb, org.jooq.Record9<java.lang.Long, java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Long>, io.cattle.platform.core.model.AuthToken {

	private static final long serialVersionUID = 801035653;

	/**
	 * Setter for <code>cattle.auth_token.id</code>.
	 */
	@Override
	public void setId(java.lang.Long value) {
		setValue(0, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.id</code>.
	 */
	@javax.persistence.Id
	@javax.persistence.Column(name = "id", unique = true, nullable = false, precision = 19)
	@Override
	public java.lang.Long getId() {
		return (java.lang.Long) getValue(0);
	}

	/**
	 * Setter for <code>cattle.auth_token.account_id</code>.
	 */
	@Override
	public void setAccountId(java.lang.Long value) {
		setValue(1, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.account_id</code>.
	 */
	@javax.persistence.Column(name = "account_id", nullable = false, precision = 19)
	@Override
	public java.lang.Long getAccountId() {
		return (java.lang.Long) getValue(1);
	}

	/**
	 * Setter for <code>cattle.auth_token.created</code>.
	 */
	@Override
	public void setCreated(java.util.Date value) {
		setValue(2, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.created</code>.
	 */
	@javax.persistence.Column(name = "created", nullable = false)
	@Override
	public java.util.Date getCreated() {
		return (java.util.Date) getValue(2);
	}

	/**
	 * Setter for <code>cattle.auth_token.expires</code>.
	 */
	@Override
	public void setExpires(java.util.Date value) {
		setValue(3, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.expires</code>.
	 */
	@javax.persistence.Column(name = "expires", nullable = false)
	@Override
	public java.util.Date getExpires() {
		return (java.util.Date) getValue(3);
	}

	/**
	 * Setter for <code>cattle.auth_token.key</code>.
	 */
	@Override
	public void setKey(java.lang.String value) {
		setValue(4, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.key</code>.
	 */
	@javax.persistence.Column(name = "key", unique = true, nullable = false, length = 40)
	@Override
	public java.lang.String getKey() {
		return (java.lang.String) getValue(4);
	}

	/**
	 * Setter for <code>cattle.auth_token.value</code>.
	 */
	@Override
	public void setValue(java.lang.String value) {
		setValue(5, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.value</code>.
	 */
	@javax.persistence.Column(name = "value", nullable = false, length = 16777215)
	@Override
	public java.lang.String getValue() {
		return (java.lang.String) getValue(5);
	}

	/**
	 * Setter for <code>cattle.auth_token.version</code>.
	 */
	@Override
	public void setVersion(java.lang.String value) {
		setValue(6, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.version</code>.
	 */
	@javax.persistence.Column(name = "version", nullable = false, length = 255)
	@Override
	public java.lang.String getVersion() {
		return (java.lang.String) getValue(6);
	}

	/**
	 * Setter for <code>cattle.auth_token.provider</code>.
	 */
	@Override
	public void setProvider(java.lang.String value) {
		setValue(7, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.provider</code>.
	 */
	@javax.persistence.Column(name = "provider", nullable = false, length = 255)
	@Override
	public java.lang.String getProvider() {
		return (java.lang.String) getValue(7);
	}

	/**
	 * Setter for <code>cattle.auth_token.authenticated_as_account_id</code>.
	 */
	@Override
	public void setAuthenticatedAsAccountId(java.lang.Long value) {
		setValue(8, value);
	}

	/**
	 * Getter for <code>cattle.auth_token.authenticated_as_account_id</code>.
	 */
	@javax.persistence.Column(name = "authenticated_as_account_id", precision = 19)
	@Override
	public java.lang.Long getAuthenticatedAsAccountId() {
		return (java.lang.Long) getValue(8);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Record1<java.lang.Long> key() {
		return (org.jooq.Record1) super.key();
	}

	// -------------------------------------------------------------------------
	// Record9 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row9<java.lang.Long, java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Long> fieldsRow() {
		return (org.jooq.Row9) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row9<java.lang.Long, java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Long> valuesRow() {
		return (org.jooq.Row9) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Long> field1() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Long> field2() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.ACCOUNT_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.util.Date> field3() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.CREATED;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.util.Date> field4() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.EXPIRES;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field5() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.KEY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field6() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.VALUE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field7() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.VERSION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field8() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.PROVIDER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Long> field9() {
		return io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN.AUTHENTICATED_AS_ACCOUNT_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Long value1() {
		return getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Long value2() {
		return getAccountId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.Date value3() {
		return getCreated();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.Date value4() {
		return getExpires();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value5() {
		return getKey();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value6() {
		return getValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value7() {
		return getVersion();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value8() {
		return getProvider();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Long value9() {
		return getAuthenticatedAsAccountId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value1(java.lang.Long value) {
		setId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value2(java.lang.Long value) {
		setAccountId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value3(java.util.Date value) {
		setCreated(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value4(java.util.Date value) {
		setExpires(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value5(java.lang.String value) {
		setKey(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value6(java.lang.String value) {
		setValue(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value7(java.lang.String value) {
		setVersion(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value8(java.lang.String value) {
		setProvider(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord value9(java.lang.Long value) {
		setAuthenticatedAsAccountId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AuthTokenRecord values(java.lang.Long value1, java.lang.Long value2, java.util.Date value3, java.util.Date value4, java.lang.String value5, java.lang.String value6, java.lang.String value7, java.lang.String value8, java.lang.Long value9) {
		return this;
	}

	// -------------------------------------------------------------------------
	// FROM and INTO
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void from(io.cattle.platform.core.model.AuthToken from) {
		setId(from.getId());
		setAccountId(from.getAccountId());
		setCreated(from.getCreated());
		setExpires(from.getExpires());
		setKey(from.getKey());
		setValue(from.getValue());
		setVersion(from.getVersion());
		setProvider(from.getProvider());
		setAuthenticatedAsAccountId(from.getAuthenticatedAsAccountId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends io.cattle.platform.core.model.AuthToken> E into(E into) {
		into.from(this);
		return into;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached AuthTokenRecord
	 */
	public AuthTokenRecord() {
		super(io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN);
	}

	/**
	 * Create a detached, initialised AuthTokenRecord
	 */
	public AuthTokenRecord(java.lang.Long id, java.lang.Long accountId, java.util.Date created, java.util.Date expires, java.lang.String key, java.lang.String value, java.lang.String version, java.lang.String provider, java.lang.Long authenticatedAsAccountId) {
		super(io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN);

		setValue(0, id);
		setValue(1, accountId);
		setValue(2, created);
		setValue(3, expires);
		setValue(4, key);
		setValue(5, value);
		setValue(6, version);
		setValue(7, provider);
		setValue(8, authenticatedAsAccountId);
	}
}
