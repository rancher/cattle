/**
 * This class is generated by jOOQ
 */
package io.cattle.platform.core.model.tables.records;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value = { "http://www.jooq.org", "3.3.0" }, comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
@javax.persistence.Entity
@javax.persistence.Table(name = "network_service", schema = "cattle")
public class NetworkServiceRecord extends org.jooq.impl.UpdatableRecordImpl<io.cattle.platform.core.model.tables.records.NetworkServiceRecord>
        implements
        io.cattle.platform.db.jooq.utils.TableRecordJaxb,
        org.jooq.Record13<java.lang.Long, java.lang.String, java.lang.Long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date, java.util.Date, java.util.Date, java.util.Map<String, Object>, java.lang.Long, java.lang.Long>,
        io.cattle.platform.core.model.NetworkService {

    private static final long serialVersionUID = -984029677;

    /**
     * Setter for <code>cattle.network_service.id</code>.
     */
    @Override
    public void setId(java.lang.Long value) {
        setValue(0, value);
    }

    /**
     * Getter for <code>cattle.network_service.id</code>.
     */
    @javax.persistence.Id
    @javax.persistence.Column(name = "id", unique = true, nullable = false, precision = 19)
    @Override
    public java.lang.Long getId() {
        return (java.lang.Long) getValue(0);
    }

    /**
     * Setter for <code>cattle.network_service.name</code>.
     */
    @Override
    public void setName(java.lang.String value) {
        setValue(1, value);
    }

    /**
     * Getter for <code>cattle.network_service.name</code>.
     */
    @javax.persistence.Column(name = "name", length = 255)
    @Override
    public java.lang.String getName() {
        return (java.lang.String) getValue(1);
    }

    /**
     * Setter for <code>cattle.network_service.account_id</code>.
     */
    @Override
    public void setAccountId(java.lang.Long value) {
        setValue(2, value);
    }

    /**
     * Getter for <code>cattle.network_service.account_id</code>.
     */
    @javax.persistence.Column(name = "account_id", precision = 19)
    @Override
    public java.lang.Long getAccountId() {
        return (java.lang.Long) getValue(2);
    }

    /**
     * Setter for <code>cattle.network_service.kind</code>.
     */
    @Override
    public void setKind(java.lang.String value) {
        setValue(3, value);
    }

    /**
     * Getter for <code>cattle.network_service.kind</code>.
     */
    @javax.persistence.Column(name = "kind", nullable = false, length = 255)
    @Override
    public java.lang.String getKind() {
        return (java.lang.String) getValue(3);
    }

    /**
     * Setter for <code>cattle.network_service.uuid</code>.
     */
    @Override
    public void setUuid(java.lang.String value) {
        setValue(4, value);
    }

    /**
     * Getter for <code>cattle.network_service.uuid</code>.
     */
    @javax.persistence.Column(name = "uuid", unique = true, nullable = false, length = 128)
    @Override
    public java.lang.String getUuid() {
        return (java.lang.String) getValue(4);
    }

    /**
     * Setter for <code>cattle.network_service.description</code>.
     */
    @Override
    public void setDescription(java.lang.String value) {
        setValue(5, value);
    }

    /**
     * Getter for <code>cattle.network_service.description</code>.
     */
    @javax.persistence.Column(name = "description", length = 1024)
    @Override
    public java.lang.String getDescription() {
        return (java.lang.String) getValue(5);
    }

    /**
     * Setter for <code>cattle.network_service.state</code>.
     */
    @Override
    public void setState(java.lang.String value) {
        setValue(6, value);
    }

    /**
     * Getter for <code>cattle.network_service.state</code>.
     */
    @javax.persistence.Column(name = "state", nullable = false, length = 128)
    @Override
    public java.lang.String getState() {
        return (java.lang.String) getValue(6);
    }

    /**
     * Setter for <code>cattle.network_service.created</code>.
     */
    @Override
    public void setCreated(java.util.Date value) {
        setValue(7, value);
    }

    /**
     * Getter for <code>cattle.network_service.created</code>.
     */
    @javax.persistence.Column(name = "created")
    @Override
    public java.util.Date getCreated() {
        return (java.util.Date) getValue(7);
    }

    /**
     * Setter for <code>cattle.network_service.removed</code>.
     */
    @Override
    public void setRemoved(java.util.Date value) {
        setValue(8, value);
    }

    /**
     * Getter for <code>cattle.network_service.removed</code>.
     */
    @javax.persistence.Column(name = "removed")
    @Override
    public java.util.Date getRemoved() {
        return (java.util.Date) getValue(8);
    }

    /**
     * Setter for <code>cattle.network_service.remove_time</code>.
     */
    @Override
    public void setRemoveTime(java.util.Date value) {
        setValue(9, value);
    }

    /**
     * Getter for <code>cattle.network_service.remove_time</code>.
     */
    @javax.persistence.Column(name = "remove_time")
    @Override
    public java.util.Date getRemoveTime() {
        return (java.util.Date) getValue(9);
    }

    /**
     * Setter for <code>cattle.network_service.data</code>.
     */
    @Override
    public void setData(java.util.Map<String, Object> value) {
        setValue(10, value);
    }

    /**
     * Getter for <code>cattle.network_service.data</code>.
     */
    @javax.persistence.Column(name = "data", length = 65535)
    @Override
    public java.util.Map<String, Object> getData() {
        return (java.util.Map<String, Object>) getValue(10);
    }

    /**
     * Setter for <code>cattle.network_service.network_id</code>.
     */
    @Override
    public void setNetworkId(java.lang.Long value) {
        setValue(11, value);
    }

    /**
     * Getter for <code>cattle.network_service.network_id</code>.
     */
    @javax.persistence.Column(name = "network_id", precision = 19)
    @Override
    public java.lang.Long getNetworkId() {
        return (java.lang.Long) getValue(11);
    }

    /**
     * Setter for
     * <code>cattle.network_service.network_service_provider_id</code>.
     */
    @Override
    public void setNetworkServiceProviderId(java.lang.Long value) {
        setValue(12, value);
    }

    /**
     * Getter for
     * <code>cattle.network_service.network_service_provider_id</code>.
     */
    @javax.persistence.Column(name = "network_service_provider_id", precision = 19)
    @Override
    public java.lang.Long getNetworkServiceProviderId() {
        return (java.lang.Long) getValue(12);
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
    // Record13 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Row13<java.lang.Long, java.lang.String, java.lang.Long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date, java.util.Date, java.util.Date, java.util.Map<String, Object>, java.lang.Long, java.lang.Long> fieldsRow() {
        return (org.jooq.Row13) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Row13<java.lang.Long, java.lang.String, java.lang.Long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date, java.util.Date, java.util.Date, java.util.Map<String, Object>, java.lang.Long, java.lang.Long> valuesRow() {
        return (org.jooq.Row13) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.Long> field1() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.String> field2() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.Long> field3() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.ACCOUNT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.String> field4() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.KIND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.String> field5() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.UUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.String> field6() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.String> field7() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.STATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.util.Date> field8() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.CREATED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.util.Date> field9() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.REMOVED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.util.Date> field10() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.REMOVE_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.util.Map<String, Object>> field11() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.DATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.Long> field12() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.NETWORK_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.Long> field13() {
        return io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID;
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
    public java.lang.String value2() {
        return getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.Long value3() {
        return getAccountId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.String value4() {
        return getKind();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.String value5() {
        return getUuid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.String value6() {
        return getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.String value7() {
        return getState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Date value8() {
        return getCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Date value9() {
        return getRemoved();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Date value10() {
        return getRemoveTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Map<String, Object> value11() {
        return getData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.Long value12() {
        return getNetworkId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.Long value13() {
        return getNetworkServiceProviderId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value1(java.lang.Long value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value2(java.lang.String value) {
        setName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value3(java.lang.Long value) {
        setAccountId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value4(java.lang.String value) {
        setKind(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value5(java.lang.String value) {
        setUuid(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value6(java.lang.String value) {
        setDescription(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value7(java.lang.String value) {
        setState(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value8(java.util.Date value) {
        setCreated(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value9(java.util.Date value) {
        setRemoved(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value10(java.util.Date value) {
        setRemoveTime(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value11(java.util.Map<String, Object> value) {
        setData(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value12(java.lang.Long value) {
        setNetworkId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord value13(java.lang.Long value) {
        setNetworkServiceProviderId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkServiceRecord values(java.lang.Long value1, java.lang.String value2, java.lang.Long value3, java.lang.String value4, java.lang.String value5,
            java.lang.String value6, java.lang.String value7, java.util.Date value8, java.util.Date value9, java.util.Date value10,
            java.util.Map<String, Object> value11, java.lang.Long value12, java.lang.Long value13) {
        return this;
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void from(io.cattle.platform.core.model.NetworkService from) {
        setId(from.getId());
        setName(from.getName());
        setAccountId(from.getAccountId());
        setKind(from.getKind());
        setUuid(from.getUuid());
        setDescription(from.getDescription());
        setState(from.getState());
        setCreated(from.getCreated());
        setRemoved(from.getRemoved());
        setRemoveTime(from.getRemoveTime());
        setData(from.getData());
        setNetworkId(from.getNetworkId());
        setNetworkServiceProviderId(from.getNetworkServiceProviderId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends io.cattle.platform.core.model.NetworkService> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached NetworkServiceRecord
     */
    public NetworkServiceRecord() {
        super(io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE);
    }

    /**
     * Create a detached, initialised NetworkServiceRecord
     */
    public NetworkServiceRecord(java.lang.Long id, java.lang.String name, java.lang.Long accountId, java.lang.String kind, java.lang.String uuid,
            java.lang.String description, java.lang.String state, java.util.Date created, java.util.Date removed, java.util.Date removeTime,
            java.util.Map<String, Object> data, java.lang.Long networkId, java.lang.Long networkServiceProviderId) {
        super(io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE);

        setValue(0, id);
        setValue(1, name);
        setValue(2, accountId);
        setValue(3, kind);
        setValue(4, uuid);
        setValue(5, description);
        setValue(6, state);
        setValue(7, created);
        setValue(8, removed);
        setValue(9, removeTime);
        setValue(10, data);
        setValue(11, networkId);
        setValue(12, networkServiceProviderId);
    }
}
