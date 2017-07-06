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
@Table(name = "service", schema = "cattle")
public interface Service extends Serializable {

    /**
     * Setter for <code>cattle.service.id</code>.
     */
    public void setId(Long value);

    /**
     * Getter for <code>cattle.service.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    public Long getId();

    /**
     * Setter for <code>cattle.service.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>cattle.service.name</code>.
     */
    @Column(name = "name", length = 255)
    public String getName();

    /**
     * Setter for <code>cattle.service.account_id</code>.
     */
    public void setAccountId(Long value);

    /**
     * Getter for <code>cattle.service.account_id</code>.
     */
    @Column(name = "account_id", precision = 19)
    public Long getAccountId();

    /**
     * Setter for <code>cattle.service.kind</code>.
     */
    public void setKind(String value);

    /**
     * Getter for <code>cattle.service.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    public String getKind();

    /**
     * Setter for <code>cattle.service.uuid</code>.
     */
    public void setUuid(String value);

    /**
     * Getter for <code>cattle.service.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    public String getUuid();

    /**
     * Setter for <code>cattle.service.description</code>.
     */
    public void setDescription(String value);

    /**
     * Getter for <code>cattle.service.description</code>.
     */
    @Column(name = "description", length = 1024)
    public String getDescription();

    /**
     * Setter for <code>cattle.service.state</code>.
     */
    public void setState(String value);

    /**
     * Getter for <code>cattle.service.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    public String getState();

    /**
     * Setter for <code>cattle.service.created</code>.
     */
    public void setCreated(Date value);

    /**
     * Getter for <code>cattle.service.created</code>.
     */
    @Column(name = "created")
    public Date getCreated();

    /**
     * Setter for <code>cattle.service.removed</code>.
     */
    public void setRemoved(Date value);

    /**
     * Getter for <code>cattle.service.removed</code>.
     */
    @Column(name = "removed")
    public Date getRemoved();

    /**
     * Setter for <code>cattle.service.remove_time</code>.
     */
    public void setRemoveTime(Date value);

    /**
     * Getter for <code>cattle.service.remove_time</code>.
     */
    @Column(name = "remove_time")
    public Date getRemoveTime();

    /**
     * Setter for <code>cattle.service.data</code>.
     */
    public void setData(Map<String,Object> value);

    /**
     * Getter for <code>cattle.service.data</code>.
     */
    @Column(name = "data", length = 16777215)
    public Map<String,Object> getData();

    /**
     * Setter for <code>cattle.service.environment_id</code>.
     */
    public void setStackId(Long value);

    /**
     * Getter for <code>cattle.service.environment_id</code>.
     */
    @Column(name = "environment_id", precision = 19)
    public Long getStackId();

    /**
     * Setter for <code>cattle.service.vip</code>.
     */
    public void setVip(String value);

    /**
     * Getter for <code>cattle.service.vip</code>.
     */
    @Column(name = "vip", length = 255)
    public String getVip();

    /**
     * Setter for <code>cattle.service.create_index</code>.
     */
    public void setCreateIndex(Long value);

    /**
     * Getter for <code>cattle.service.create_index</code>.
     */
    @Column(name = "create_index", precision = 19)
    public Long getCreateIndex();

    /**
     * Setter for <code>cattle.service.selector</code>.
     */
    public void setSelector(String value);

    /**
     * Getter for <code>cattle.service.selector</code>.
     */
    @Column(name = "selector", length = 4096)
    public String getSelector();

    /**
     * Setter for <code>cattle.service.external_id</code>.
     */
    public void setExternalId(String value);

    /**
     * Getter for <code>cattle.service.external_id</code>.
     */
    @Column(name = "external_id", length = 255)
    public String getExternalId();

    /**
     * Setter for <code>cattle.service.health_state</code>.
     */
    public void setHealthState(String value);

    /**
     * Getter for <code>cattle.service.health_state</code>.
     */
    @Column(name = "health_state", length = 128)
    public String getHealthState();

    /**
     * Setter for <code>cattle.service.system</code>.
     */
    public void setSystem(Boolean value);

    /**
     * Getter for <code>cattle.service.system</code>.
     */
    @Column(name = "system", nullable = false, precision = 1)
    public Boolean getSystem();

    /**
     * Setter for <code>cattle.service.previous_revision_id</code>.
     */
    public void setPreviousRevisionId(Long value);

    /**
     * Getter for <code>cattle.service.previous_revision_id</code>.
     */
    @Column(name = "previous_revision_id", precision = 19)
    public Long getPreviousRevisionId();

    /**
     * Setter for <code>cattle.service.revision_id</code>.
     */
    public void setRevisionId(Long value);

    /**
     * Getter for <code>cattle.service.revision_id</code>.
     */
    @Column(name = "revision_id", precision = 19)
    public Long getRevisionId();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface Service
     */
    public void from(io.cattle.platform.core.model.Service from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface Service
     */
    public <E extends io.cattle.platform.core.model.Service> E into(E into);
}