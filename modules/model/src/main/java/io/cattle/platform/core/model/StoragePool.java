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
@Table(name = "storage_pool", schema = "cattle")
public interface StoragePool extends Serializable {

    /**
     * Setter for <code>cattle.storage_pool.id</code>.
     */
    public void setId(Long value);

    /**
     * Getter for <code>cattle.storage_pool.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    public Long getId();

    /**
     * Setter for <code>cattle.storage_pool.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>cattle.storage_pool.name</code>.
     */
    @Column(name = "name", length = 255)
    public String getName();

    /**
     * Setter for <code>cattle.storage_pool.kind</code>.
     */
    public void setKind(String value);

    /**
     * Getter for <code>cattle.storage_pool.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    public String getKind();

    /**
     * Setter for <code>cattle.storage_pool.uuid</code>.
     */
    public void setUuid(String value);

    /**
     * Getter for <code>cattle.storage_pool.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    public String getUuid();

    /**
     * Setter for <code>cattle.storage_pool.description</code>.
     */
    public void setDescription(String value);

    /**
     * Getter for <code>cattle.storage_pool.description</code>.
     */
    @Column(name = "description", length = 1024)
    public String getDescription();

    /**
     * Setter for <code>cattle.storage_pool.state</code>.
     */
    public void setState(String value);

    /**
     * Getter for <code>cattle.storage_pool.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    public String getState();

    /**
     * Setter for <code>cattle.storage_pool.created</code>.
     */
    public void setCreated(Date value);

    /**
     * Getter for <code>cattle.storage_pool.created</code>.
     */
    @Column(name = "created")
    public Date getCreated();

    /**
     * Setter for <code>cattle.storage_pool.removed</code>.
     */
    public void setRemoved(Date value);

    /**
     * Getter for <code>cattle.storage_pool.removed</code>.
     */
    @Column(name = "removed")
    public Date getRemoved();

    /**
     * Setter for <code>cattle.storage_pool.remove_time</code>.
     */
    public void setRemoveTime(Date value);

    /**
     * Getter for <code>cattle.storage_pool.remove_time</code>.
     */
    @Column(name = "remove_time")
    public Date getRemoveTime();

    /**
     * Setter for <code>cattle.storage_pool.data</code>.
     */
    public void setData(Map<String,Object> value);

    /**
     * Getter for <code>cattle.storage_pool.data</code>.
     */
    @Column(name = "data", length = 16777215)
    public Map<String,Object> getData();

    /**
     * Setter for <code>cattle.storage_pool.physical_total_size_mb</code>.
     */
    public void setPhysicalTotalSizeMb(Long value);

    /**
     * Getter for <code>cattle.storage_pool.physical_total_size_mb</code>.
     */
    @Column(name = "physical_total_size_mb", precision = 19)
    public Long getPhysicalTotalSizeMb();

    /**
     * Setter for <code>cattle.storage_pool.virtual_total_size_mb</code>.
     */
    public void setVirtualTotalSizeMb(Long value);

    /**
     * Getter for <code>cattle.storage_pool.virtual_total_size_mb</code>.
     */
    @Column(name = "virtual_total_size_mb", precision = 19)
    public Long getVirtualTotalSizeMb();

    /**
     * Setter for <code>cattle.storage_pool.external</code>.
     */
    public void setExternal(Boolean value);

    /**
     * Getter for <code>cattle.storage_pool.external</code>.
     */
    @Column(name = "external", nullable = false, precision = 1)
    public Boolean getExternal();

    /**
     * Setter for <code>cattle.storage_pool.agent_id</code>.
     */
    public void setAgentId(Long value);

    /**
     * Getter for <code>cattle.storage_pool.agent_id</code>.
     */
    @Column(name = "agent_id", precision = 19)
    public Long getAgentId();

    /**
     * Setter for <code>cattle.storage_pool.zone_id</code>.
     */
    public void setZoneId(Long value);

    /**
     * Getter for <code>cattle.storage_pool.zone_id</code>.
     */
    @Column(name = "zone_id", precision = 19)
    public Long getZoneId();

    /**
     * Setter for <code>cattle.storage_pool.external_id</code>.
     */
    public void setExternalId(String value);

    /**
     * Getter for <code>cattle.storage_pool.external_id</code>.
     */
    @Column(name = "external_id", length = 128)
    public String getExternalId();

    /**
     * Setter for <code>cattle.storage_pool.driver_name</code>.
     */
    public void setDriverName(String value);

    /**
     * Getter for <code>cattle.storage_pool.driver_name</code>.
     */
    @Column(name = "driver_name", length = 255)
    public String getDriverName();

    /**
     * Setter for <code>cattle.storage_pool.volume_access_mode</code>.
     */
    public void setVolumeAccessMode(String value);

    /**
     * Getter for <code>cattle.storage_pool.volume_access_mode</code>.
     */
    @Column(name = "volume_access_mode", length = 255)
    public String getVolumeAccessMode();

    /**
     * Setter for <code>cattle.storage_pool.storage_driver_id</code>.
     */
    public void setStorageDriverId(Long value);

    /**
     * Getter for <code>cattle.storage_pool.storage_driver_id</code>.
     */
    @Column(name = "storage_driver_id", precision = 19)
    public Long getStorageDriverId();

    /**
     * Setter for <code>cattle.storage_pool.cluster_id</code>.
     */
    public void setClusterId(Long value);

    /**
     * Getter for <code>cattle.storage_pool.cluster_id</code>.
     */
    @Column(name = "cluster_id", nullable = false, precision = 19)
    public Long getClusterId();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface StoragePool
     */
    public void from(io.cattle.platform.core.model.StoragePool from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface StoragePool
     */
    public <E extends io.cattle.platform.core.model.StoragePool> E into(E into);
}
