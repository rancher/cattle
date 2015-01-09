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
@javax.persistence.Table(name = "data", schema = "cattle")
public interface Data extends java.io.Serializable {

    /**
     * Setter for <code>cattle.data.id</code>.
     */
    public void setId(java.lang.Long value);

    /**
     * Getter for <code>cattle.data.id</code>.
     */
    @javax.persistence.Id
    @javax.persistence.Column(name = "id", unique = true, nullable = false, precision = 19)
    public java.lang.Long getId();

    /**
     * Setter for <code>cattle.data.name</code>.
     */
    public void setName(java.lang.String value);

    /**
     * Getter for <code>cattle.data.name</code>.
     */
    @javax.persistence.Column(name = "name", unique = true, nullable = false, length = 255)
    public java.lang.String getName();

    /**
     * Setter for <code>cattle.data.visible</code>.
     */
    public void setVisible(java.lang.Boolean value);

    /**
     * Getter for <code>cattle.data.visible</code>.
     */
    @javax.persistence.Column(name = "visible", nullable = false, precision = 1)
    public java.lang.Boolean getVisible();

    /**
     * Setter for <code>cattle.data.value</code>.
     */
    public void setValue(java.lang.String value);

    /**
     * Getter for <code>cattle.data.value</code>.
     */
    @javax.persistence.Column(name = "value", nullable = false, length = 65535)
    public java.lang.String getValue();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common
     * interface Data
     */
    public void from(io.cattle.platform.core.model.Data from);

    /**
     * Copy data into another generated Record/POJO implementing the common
     * interface Data
     */
    public <E extends io.cattle.platform.core.model.Data> E into(E into);
}
