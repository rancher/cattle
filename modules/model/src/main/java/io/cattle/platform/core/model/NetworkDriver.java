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
@Table(name = "network_driver", schema = "cattle")
public interface NetworkDriver extends Serializable {

    /**
     * Setter for <code>cattle.network_driver.id</code>.
     */
    public void setId(Long value);

    /**
     * Getter for <code>cattle.network_driver.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    public Long getId();

    /**
     * Setter for <code>cattle.network_driver.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>cattle.network_driver.name</code>.
     */
    @Column(name = "name", length = 255)
    public String getName();

    /**
     * Setter for <code>cattle.network_driver.kind</code>.
     */
    public void setKind(String value);

    /**
     * Getter for <code>cattle.network_driver.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    public String getKind();

    /**
     * Setter for <code>cattle.network_driver.uuid</code>.
     */
    public void setUuid(String value);

    /**
     * Getter for <code>cattle.network_driver.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    public String getUuid();

    /**
     * Setter for <code>cattle.network_driver.description</code>.
     */
    public void setDescription(String value);

    /**
     * Getter for <code>cattle.network_driver.description</code>.
     */
    @Column(name = "description", length = 1024)
    public String getDescription();

    /**
     * Setter for <code>cattle.network_driver.state</code>.
     */
    public void setState(String value);

    /**
     * Getter for <code>cattle.network_driver.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    public String getState();

    /**
     * Setter for <code>cattle.network_driver.created</code>.
     */
    public void setCreated(Date value);

    /**
     * Getter for <code>cattle.network_driver.created</code>.
     */
    @Column(name = "created")
    public Date getCreated();

    /**
     * Setter for <code>cattle.network_driver.removed</code>.
     */
    public void setRemoved(Date value);

    /**
     * Getter for <code>cattle.network_driver.removed</code>.
     */
    @Column(name = "removed")
    public Date getRemoved();

    /**
     * Setter for <code>cattle.network_driver.remove_time</code>.
     */
    public void setRemoveTime(Date value);

    /**
     * Getter for <code>cattle.network_driver.remove_time</code>.
     */
    @Column(name = "remove_time")
    public Date getRemoveTime();

    /**
     * Setter for <code>cattle.network_driver.data</code>.
     */
    public void setData(Map<String,Object> value);

    /**
     * Getter for <code>cattle.network_driver.data</code>.
     */
    @Column(name = "data", length = 16777215)
    public Map<String,Object> getData();

    /**
     * Setter for <code>cattle.network_driver.service_id</code>.
     */
    public void setServiceId(Long value);

    /**
     * Getter for <code>cattle.network_driver.service_id</code>.
     */
    @Column(name = "service_id", precision = 19)
    public Long getServiceId();

    /**
     * Setter for <code>cattle.network_driver.cluster_id</code>.
     */
    public void setClusterId(Long value);

    /**
     * Getter for <code>cattle.network_driver.cluster_id</code>.
     */
    @Column(name = "cluster_id", nullable = false, precision = 19)
    public Long getClusterId();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface NetworkDriver
     */
    public void from(io.cattle.platform.core.model.NetworkDriver from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface NetworkDriver
     */
    public <E extends io.cattle.platform.core.model.NetworkDriver> E into(E into);
}
