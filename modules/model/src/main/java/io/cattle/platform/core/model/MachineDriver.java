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
@Table(name = "machine_driver", schema = "cattle")
public interface MachineDriver extends Serializable {

    /**
     * Setter for <code>cattle.machine_driver.id</code>.
     */
    public void setId(Long value);

    /**
     * Getter for <code>cattle.machine_driver.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    public Long getId();

    /**
     * Setter for <code>cattle.machine_driver.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>cattle.machine_driver.name</code>.
     */
    @Column(name = "name", length = 255)
    public String getName();

    /**
     * Setter for <code>cattle.machine_driver.kind</code>.
     */
    public void setKind(String value);

    /**
     * Getter for <code>cattle.machine_driver.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    public String getKind();

    /**
     * Setter for <code>cattle.machine_driver.uuid</code>.
     */
    public void setUuid(String value);

    /**
     * Getter for <code>cattle.machine_driver.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    public String getUuid();

    /**
     * Setter for <code>cattle.machine_driver.description</code>.
     */
    public void setDescription(String value);

    /**
     * Getter for <code>cattle.machine_driver.description</code>.
     */
    @Column(name = "description", length = 1024)
    public String getDescription();

    /**
     * Setter for <code>cattle.machine_driver.state</code>.
     */
    public void setState(String value);

    /**
     * Getter for <code>cattle.machine_driver.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    public String getState();

    /**
     * Setter for <code>cattle.machine_driver.created</code>.
     */
    public void setCreated(Date value);

    /**
     * Getter for <code>cattle.machine_driver.created</code>.
     */
    @Column(name = "created")
    public Date getCreated();

    /**
     * Setter for <code>cattle.machine_driver.removed</code>.
     */
    public void setRemoved(Date value);

    /**
     * Getter for <code>cattle.machine_driver.removed</code>.
     */
    @Column(name = "removed")
    public Date getRemoved();

    /**
     * Setter for <code>cattle.machine_driver.remove_time</code>.
     */
    public void setRemoveTime(Date value);

    /**
     * Getter for <code>cattle.machine_driver.remove_time</code>.
     */
    @Column(name = "remove_time")
    public Date getRemoveTime();

    /**
     * Setter for <code>cattle.machine_driver.data</code>.
     */
    public void setData(Map<String,Object> value);

    /**
     * Getter for <code>cattle.machine_driver.data</code>.
     */
    @Column(name = "data", length = 65535)
    public Map<String,Object> getData();

    /**
     * Setter for <code>cattle.machine_driver.uri</code>.
     */
    public void setUri(String value);

    /**
     * Getter for <code>cattle.machine_driver.uri</code>.
     */
    @Column(name = "uri", length = 255)
    public String getUri();

    /**
     * Setter for <code>cattle.machine_driver.md5checksum</code>.
     */
    public void setMd5checksum(String value);

    /**
     * Getter for <code>cattle.machine_driver.md5checksum</code>.
     */
    @Column(name = "md5checksum", length = 255)
    public String getMd5checksum();

    /**
     * Setter for <code>cattle.machine_driver.creator_id</code>.
     */
    public void setCreatorId(Long value);

    /**
     * Getter for <code>cattle.machine_driver.creator_id</code>.
     */
    @Column(name = "creator_id", precision = 19)
    public Long getCreatorId();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface MachineDriver
     */
    public void from(io.cattle.platform.core.model.MachineDriver from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface MachineDriver
     */
    public <E extends io.cattle.platform.core.model.MachineDriver> E into(E into);
}
