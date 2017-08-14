/*
 * This file is generated by jOOQ.
*/
package io.cattle.platform.core.model;


import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.annotation.Generated;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
@Entity
@Table(name = "secret", schema = "cattle")
public interface Secret extends Serializable {

    /**
     * Setter for <code>cattle.secret.id</code>.
     */
    public void setId(Long value);

    /**
     * Getter for <code>cattle.secret.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    public Long getId();

    /**
     * Setter for <code>cattle.secret.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>cattle.secret.name</code>.
     */
    @Column(name = "name", length = 255)
    public String getName();

    /**
     * Setter for <code>cattle.secret.account_id</code>.
     */
    public void setAccountId(Long value);

    /**
     * Getter for <code>cattle.secret.account_id</code>.
     */
    @Column(name = "account_id", precision = 19)
    public Long getAccountId();

    /**
     * Setter for <code>cattle.secret.kind</code>.
     */
    public void setKind(String value);

    /**
     * Getter for <code>cattle.secret.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    public String getKind();

    /**
     * Setter for <code>cattle.secret.uuid</code>.
     */
    public void setUuid(String value);

    /**
     * Getter for <code>cattle.secret.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    public String getUuid();

    /**
     * Setter for <code>cattle.secret.description</code>.
     */
    public void setDescription(String value);

    /**
     * Getter for <code>cattle.secret.description</code>.
     */
    @Column(name = "description", length = 1024)
    public String getDescription();

    /**
     * Setter for <code>cattle.secret.state</code>.
     */
    public void setState(String value);

    /**
     * Getter for <code>cattle.secret.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    public String getState();

    /**
     * Setter for <code>cattle.secret.created</code>.
     */
    public void setCreated(Date value);

    /**
     * Getter for <code>cattle.secret.created</code>.
     */
    @Column(name = "created")
    public Date getCreated();

    /**
     * Setter for <code>cattle.secret.removed</code>.
     */
    public void setRemoved(Date value);

    /**
     * Getter for <code>cattle.secret.removed</code>.
     */
    @Column(name = "removed")
    public Date getRemoved();

    /**
     * Setter for <code>cattle.secret.remove_time</code>.
     */
    public void setRemoveTime(Date value);

    /**
     * Getter for <code>cattle.secret.remove_time</code>.
     */
    @Column(name = "remove_time")
    public Date getRemoveTime();

    /**
     * Setter for <code>cattle.secret.data</code>.
     */
    public void setData(Map<String,Object> value);

    /**
     * Getter for <code>cattle.secret.data</code>.
     */
    @Column(name = "data", length = 16777215)
    public Map<String,Object> getData();

    /**
     * Setter for <code>cattle.secret.value</code>.
     */
    public void setValue(String value);

    /**
     * Getter for <code>cattle.secret.value</code>.
     */
    @Column(name = "value", length = 16777215)
    public String getValue();

    /**
     * Setter for <code>cattle.secret.environment_id</code>.
     */
    public void setStackId(Long value);

    /**
     * Getter for <code>cattle.secret.environment_id</code>.
     */
    @Column(name = "environment_id", precision = 19)
    public Long getStackId();

    /**
     * Setter for <code>cattle.secret.creator_id</code>.
     */
    public void setCreatorId(Long value);

    /**
     * Getter for <code>cattle.secret.creator_id</code>.
     */
    @Column(name = "creator_id", precision = 19)
    public Long getCreatorId();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface Secret
     */
    public void from(io.cattle.platform.core.model.Secret from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface Secret
     */
    public <E extends io.cattle.platform.core.model.Secret> E into(E into);
}
