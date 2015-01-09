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
@javax.persistence.Table(name = "DATABASECHANGELOG", schema = "cattle")
public interface Databasechangelog extends java.io.Serializable {

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.ID</code>.
     */
    public void setId(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.ID</code>.
     */
    @javax.persistence.Column(name = "ID", nullable = false, length = 255)
    public java.lang.String getId();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.AUTHOR</code>.
     */
    public void setAuthor(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.AUTHOR</code>.
     */
    @javax.persistence.Column(name = "AUTHOR", nullable = false, length = 255)
    public java.lang.String getAuthor();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.FILENAME</code>.
     */
    public void setFilename(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.FILENAME</code>.
     */
    @javax.persistence.Column(name = "FILENAME", nullable = false, length = 255)
    public java.lang.String getFilename();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.DATEEXECUTED</code>.
     */
    public void setDateexecuted(java.util.Date value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.DATEEXECUTED</code>.
     */
    @javax.persistence.Column(name = "DATEEXECUTED", nullable = false)
    public java.util.Date getDateexecuted();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.ORDEREXECUTED</code>.
     */
    public void setOrderexecuted(java.lang.Integer value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.ORDEREXECUTED</code>.
     */
    @javax.persistence.Column(name = "ORDEREXECUTED", nullable = false, precision = 10)
    public java.lang.Integer getOrderexecuted();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.EXECTYPE</code>.
     */
    public void setExectype(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.EXECTYPE</code>.
     */
    @javax.persistence.Column(name = "EXECTYPE", nullable = false, length = 10)
    public java.lang.String getExectype();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.MD5SUM</code>.
     */
    public void setMd5sum(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.MD5SUM</code>.
     */
    @javax.persistence.Column(name = "MD5SUM", length = 35)
    public java.lang.String getMd5sum();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.DESCRIPTION</code>.
     */
    public void setDescription(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.DESCRIPTION</code>.
     */
    @javax.persistence.Column(name = "DESCRIPTION", length = 255)
    public java.lang.String getDescription();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.COMMENTS</code>.
     */
    public void setComments(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.COMMENTS</code>.
     */
    @javax.persistence.Column(name = "COMMENTS", length = 255)
    public java.lang.String getComments();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.TAG</code>.
     */
    public void setTag(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.TAG</code>.
     */
    @javax.persistence.Column(name = "TAG", length = 255)
    public java.lang.String getTag();

    /**
     * Setter for <code>cattle.DATABASECHANGELOG.LIQUIBASE</code>.
     */
    public void setLiquibase(java.lang.String value);

    /**
     * Getter for <code>cattle.DATABASECHANGELOG.LIQUIBASE</code>.
     */
    @javax.persistence.Column(name = "LIQUIBASE", length = 20)
    public java.lang.String getLiquibase();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common
     * interface Databasechangelog
     */
    public void from(io.cattle.platform.core.model.Databasechangelog from);

    /**
     * Copy data into another generated Record/POJO implementing the common
     * interface Databasechangelog
     */
    public <E extends io.cattle.platform.core.model.Databasechangelog> E into(E into);
}
