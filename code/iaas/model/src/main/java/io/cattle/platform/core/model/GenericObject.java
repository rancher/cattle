/**
 * This class is generated by jOOQ
 */
package io.cattle.platform.core.model;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value = { "http://www.jooq.org", "3.3.0" }, comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
@javax.persistence.Entity
@javax.persistence.Table(name = "generic_object", schema = "cattle")
public interface GenericObject extends java.io.Serializable {

    /**
     * Setter for <code>cattle.generic_object.id</code>.
     */
    public void setId(java.lang.Long value);

    /**
     * Getter for <code>cattle.generic_object.id</code>.
     */
    @javax.persistence.Id
    @javax.persistence.Column(name = "id", unique = true, nullable = false, precision = 19)
    public java.lang.Long getId();

    /**
     * Setter for <code>cattle.generic_object.name</code>.
     */
    public void setName(java.lang.String value);

    /**
     * Getter for <code>cattle.generic_object.name</code>.
     */
    @javax.persistence.Column(name = "name", length = 255)
    public java.lang.String getName();

    /**
     * Setter for <code>cattle.generic_object.account_id</code>.
     */
    public void setAccountId(java.lang.Long value);

    /**
     * Getter for <code>cattle.generic_object.account_id</code>.
     */
    @javax.persistence.Column(name = "account_id", precision = 19)
    public java.lang.Long getAccountId();

    /**
     * Setter for <code>cattle.generic_object.kind</code>.
     */
    public void setKind(java.lang.String value);

    /**
     * Getter for <code>cattle.generic_object.kind</code>.
     */
    @javax.persistence.Column(name = "kind", nullable = false, length = 255)
    public java.lang.String getKind();

    /**
     * Setter for <code>cattle.generic_object.uuid</code>.
     */
    public void setUuid(java.lang.String value);

    /**
     * Getter for <code>cattle.generic_object.uuid</code>.
     */
    @javax.persistence.Column(name = "uuid", unique = true, nullable = false, length = 128)
    public java.lang.String getUuid();

    /**
     * Setter for <code>cattle.generic_object.description</code>.
     */
    public void setDescription(java.lang.String value);

    /**
     * Getter for <code>cattle.generic_object.description</code>.
     */
    @javax.persistence.Column(name = "description", length = 1024)
    public java.lang.String getDescription();

    /**
     * Setter for <code>cattle.generic_object.state</code>.
     */
    public void setState(java.lang.String value);

    /**
     * Getter for <code>cattle.generic_object.state</code>.
     */
    @javax.persistence.Column(name = "state", nullable = false, length = 128)
    public java.lang.String getState();

    /**
     * Setter for <code>cattle.generic_object.created</code>.
     */
    public void setCreated(java.util.Date value);

    /**
     * Getter for <code>cattle.generic_object.created</code>.
     */
    @javax.persistence.Column(name = "created")
    public java.util.Date getCreated();

    /**
     * Setter for <code>cattle.generic_object.removed</code>.
     */
    public void setRemoved(java.util.Date value);

    /**
     * Getter for <code>cattle.generic_object.removed</code>.
     */
    @javax.persistence.Column(name = "removed")
    public java.util.Date getRemoved();

    /**
     * Setter for <code>cattle.generic_object.remove_time</code>.
     */
    public void setRemoveTime(java.util.Date value);

    /**
     * Getter for <code>cattle.generic_object.remove_time</code>.
     */
    @javax.persistence.Column(name = "remove_time")
    public java.util.Date getRemoveTime();

    /**
     * Setter for <code>cattle.generic_object.data</code>.
     */
    public void setData(java.util.Map<String, Object> value);

    /**
     * Getter for <code>cattle.generic_object.data</code>.
     */
    @javax.persistence.Column(name = "data", length = 65535)
    public java.util.Map<String, Object> getData();

    /**
     * Setter for <code>cattle.generic_object.key</code>.
     */
    public void setKey(java.lang.String value);

    /**
     * Getter for <code>cattle.generic_object.key</code>.
     */
    @javax.persistence.Column(name = "key", length = 255)
    public java.lang.String getKey();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common
     * interface GenericObject
     */
    public void from(io.cattle.platform.core.model.GenericObject from);

    /**
     * Copy data into another generated Record/POJO implementing the common
     * interface GenericObject
     */
    public <E extends io.cattle.platform.core.model.GenericObject> E into(E into);
}
