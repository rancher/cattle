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
@javax.persistence.Table(name = "task_instance", schema = "cattle")
public class TaskInstanceRecord extends org.jooq.impl.UpdatableRecordImpl<io.cattle.platform.core.model.tables.records.TaskInstanceRecord> implements
        io.cattle.platform.db.jooq.utils.TableRecordJaxb,
        org.jooq.Record7<java.lang.Long, java.lang.String, java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String>,
        io.cattle.platform.core.model.TaskInstance {

    private static final long serialVersionUID = -266012306;

    /**
     * Setter for <code>cattle.task_instance.id</code>.
     */
    @Override
    public void setId(java.lang.Long value) {
        setValue(0, value);
    }

    /**
     * Getter for <code>cattle.task_instance.id</code>.
     */
    @javax.persistence.Id
    @javax.persistence.Column(name = "id", unique = true, nullable = false, precision = 19)
    @Override
    public java.lang.Long getId() {
        return (java.lang.Long) getValue(0);
    }

    /**
     * Setter for <code>cattle.task_instance.name</code>.
     */
    @Override
    public void setName(java.lang.String value) {
        setValue(1, value);
    }

    /**
     * Getter for <code>cattle.task_instance.name</code>.
     */
    @javax.persistence.Column(name = "name", nullable = false, length = 128)
    @Override
    public java.lang.String getName() {
        return (java.lang.String) getValue(1);
    }

    /**
     * Setter for <code>cattle.task_instance.task_id</code>.
     */
    @Override
    public void setTaskId(java.lang.Long value) {
        setValue(2, value);
    }

    /**
     * Getter for <code>cattle.task_instance.task_id</code>.
     */
    @javax.persistence.Column(name = "task_id", nullable = false, precision = 19)
    @Override
    public java.lang.Long getTaskId() {
        return (java.lang.Long) getValue(2);
    }

    /**
     * Setter for <code>cattle.task_instance.start_time</code>.
     */
    @Override
    public void setStartTime(java.util.Date value) {
        setValue(3, value);
    }

    /**
     * Getter for <code>cattle.task_instance.start_time</code>.
     */
    @javax.persistence.Column(name = "start_time", nullable = false)
    @Override
    public java.util.Date getStartTime() {
        return (java.util.Date) getValue(3);
    }

    /**
     * Setter for <code>cattle.task_instance.end_time</code>.
     */
    @Override
    public void setEndTime(java.util.Date value) {
        setValue(4, value);
    }

    /**
     * Getter for <code>cattle.task_instance.end_time</code>.
     */
    @javax.persistence.Column(name = "end_time")
    @Override
    public java.util.Date getEndTime() {
        return (java.util.Date) getValue(4);
    }

    /**
     * Setter for <code>cattle.task_instance.exception</code>.
     */
    @Override
    public void setException(java.lang.String value) {
        setValue(5, value);
    }

    /**
     * Getter for <code>cattle.task_instance.exception</code>.
     */
    @javax.persistence.Column(name = "exception", length = 255)
    @Override
    public java.lang.String getException() {
        return (java.lang.String) getValue(5);
    }

    /**
     * Setter for <code>cattle.task_instance.server_id</code>.
     */
    @Override
    public void setServerId(java.lang.String value) {
        setValue(6, value);
    }

    /**
     * Getter for <code>cattle.task_instance.server_id</code>.
     */
    @javax.persistence.Column(name = "server_id", nullable = false, length = 128)
    @Override
    public java.lang.String getServerId() {
        return (java.lang.String) getValue(6);
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
    // Record7 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Row7<java.lang.Long, java.lang.String, java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String> fieldsRow() {
        return (org.jooq.Row7) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Row7<java.lang.Long, java.lang.String, java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String> valuesRow() {
        return (org.jooq.Row7) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.Long> field1() {
        return io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.String> field2() {
        return io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE.NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.Long> field3() {
        return io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE.TASK_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.util.Date> field4() {
        return io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE.START_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.util.Date> field5() {
        return io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE.END_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.String> field6() {
        return io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE.EXCEPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Field<java.lang.String> field7() {
        return io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE.SERVER_ID;
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
        return getTaskId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Date value4() {
        return getStartTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Date value5() {
        return getEndTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.String value6() {
        return getException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.lang.String value7() {
        return getServerId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskInstanceRecord value1(java.lang.Long value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskInstanceRecord value2(java.lang.String value) {
        setName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskInstanceRecord value3(java.lang.Long value) {
        setTaskId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskInstanceRecord value4(java.util.Date value) {
        setStartTime(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskInstanceRecord value5(java.util.Date value) {
        setEndTime(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskInstanceRecord value6(java.lang.String value) {
        setException(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskInstanceRecord value7(java.lang.String value) {
        setServerId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskInstanceRecord values(java.lang.Long value1, java.lang.String value2, java.lang.Long value3, java.util.Date value4, java.util.Date value5,
            java.lang.String value6, java.lang.String value7) {
        return this;
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void from(io.cattle.platform.core.model.TaskInstance from) {
        setId(from.getId());
        setName(from.getName());
        setTaskId(from.getTaskId());
        setStartTime(from.getStartTime());
        setEndTime(from.getEndTime());
        setException(from.getException());
        setServerId(from.getServerId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends io.cattle.platform.core.model.TaskInstance> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TaskInstanceRecord
     */
    public TaskInstanceRecord() {
        super(io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE);
    }

    /**
     * Create a detached, initialised TaskInstanceRecord
     */
    public TaskInstanceRecord(java.lang.Long id, java.lang.String name, java.lang.Long taskId, java.util.Date startTime, java.util.Date endTime,
            java.lang.String exception, java.lang.String serverId) {
        super(io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE);

        setValue(0, id);
        setValue(1, name);
        setValue(2, taskId);
        setValue(3, startTime);
        setValue(4, endTime);
        setValue(5, exception);
        setValue(6, serverId);
    }
}
