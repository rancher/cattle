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
@Table(name = "account", schema = "cattle")
public interface Account extends Serializable {

    /**
     * Setter for <code>cattle.account.id</code>.
     */
    public void setId(Long value);

    /**
     * Getter for <code>cattle.account.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    public Long getId();

    /**
     * Setter for <code>cattle.account.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>cattle.account.name</code>.
     */
    @Column(name = "name", length = 255)
    public String getName();

    /**
     * Setter for <code>cattle.account.kind</code>.
     */
    public void setKind(String value);

    /**
     * Getter for <code>cattle.account.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    public String getKind();

    /**
     * Setter for <code>cattle.account.uuid</code>.
     */
    public void setUuid(String value);

    /**
     * Getter for <code>cattle.account.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    public String getUuid();

    /**
     * Setter for <code>cattle.account.description</code>.
     */
    public void setDescription(String value);

    /**
     * Getter for <code>cattle.account.description</code>.
     */
    @Column(name = "description", length = 1024)
    public String getDescription();

    /**
     * Setter for <code>cattle.account.state</code>.
     */
    public void setState(String value);

    /**
     * Getter for <code>cattle.account.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    public String getState();

    /**
     * Setter for <code>cattle.account.created</code>.
     */
    public void setCreated(Date value);

    /**
     * Getter for <code>cattle.account.created</code>.
     */
    @Column(name = "created")
    public Date getCreated();

    /**
     * Setter for <code>cattle.account.removed</code>.
     */
    public void setRemoved(Date value);

    /**
     * Getter for <code>cattle.account.removed</code>.
     */
    @Column(name = "removed")
    public Date getRemoved();

    /**
     * Setter for <code>cattle.account.remove_time</code>.
     */
    public void setRemoveTime(Date value);

    /**
     * Getter for <code>cattle.account.remove_time</code>.
     */
    @Column(name = "remove_time")
    public Date getRemoveTime();

    /**
     * Setter for <code>cattle.account.data</code>.
     */
    public void setData(Map<String,Object> value);

    /**
     * Getter for <code>cattle.account.data</code>.
     */
    @Column(name = "data", length = 16777215)
    public Map<String,Object> getData();

    /**
     * Setter for <code>cattle.account.external_id</code>.
     */
    public void setExternalId(String value);

    /**
     * Getter for <code>cattle.account.external_id</code>.
     */
    @Column(name = "external_id", length = 255)
    public String getExternalId();

    /**
     * Setter for <code>cattle.account.external_id_type</code>.
     */
    public void setExternalIdType(String value);

    /**
     * Getter for <code>cattle.account.external_id_type</code>.
     */
    @Column(name = "external_id_type", length = 128)
    public String getExternalIdType();

    /**
     * Setter for <code>cattle.account.version</code>.
     */
    public void setVersion(String value);

    /**
     * Getter for <code>cattle.account.version</code>.
     */
    @Column(name = "version", length = 128)
    public String getVersion();

    /**
     * Setter for <code>cattle.account.cluster_id</code>.
     */
    public void setClusterId(Long value);

    /**
     * Getter for <code>cattle.account.cluster_id</code>.
     */
    @Column(name = "cluster_id", precision = 19)
    public Long getClusterId();

    /**
     * Setter for <code>cattle.account.cluster_owner</code>.
     */
    public void setClusterOwner(Boolean value);

    /**
     * Getter for <code>cattle.account.cluster_owner</code>.
     */
    @Column(name = "cluster_owner", nullable = false, precision = 1)
    public Boolean getClusterOwner();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface Account
     */
    public void from(io.cattle.platform.core.model.Account from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface Account
     */
    public <E extends io.cattle.platform.core.model.Account> E into(E into);
}
