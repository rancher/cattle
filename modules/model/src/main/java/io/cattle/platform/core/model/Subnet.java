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
@Table(name = "subnet", schema = "cattle")
public interface Subnet extends Serializable {

    /**
     * Setter for <code>cattle.subnet.id</code>.
     */
    public void setId(Long value);

    /**
     * Getter for <code>cattle.subnet.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    public Long getId();

    /**
     * Setter for <code>cattle.subnet.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>cattle.subnet.name</code>.
     */
    @Column(name = "name", length = 255)
    public String getName();

    /**
     * Setter for <code>cattle.subnet.kind</code>.
     */
    public void setKind(String value);

    /**
     * Getter for <code>cattle.subnet.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    public String getKind();

    /**
     * Setter for <code>cattle.subnet.uuid</code>.
     */
    public void setUuid(String value);

    /**
     * Getter for <code>cattle.subnet.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    public String getUuid();

    /**
     * Setter for <code>cattle.subnet.description</code>.
     */
    public void setDescription(String value);

    /**
     * Getter for <code>cattle.subnet.description</code>.
     */
    @Column(name = "description", length = 1024)
    public String getDescription();

    /**
     * Setter for <code>cattle.subnet.state</code>.
     */
    public void setState(String value);

    /**
     * Getter for <code>cattle.subnet.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    public String getState();

    /**
     * Setter for <code>cattle.subnet.created</code>.
     */
    public void setCreated(Date value);

    /**
     * Getter for <code>cattle.subnet.created</code>.
     */
    @Column(name = "created")
    public Date getCreated();

    /**
     * Setter for <code>cattle.subnet.removed</code>.
     */
    public void setRemoved(Date value);

    /**
     * Getter for <code>cattle.subnet.removed</code>.
     */
    @Column(name = "removed")
    public Date getRemoved();

    /**
     * Setter for <code>cattle.subnet.remove_time</code>.
     */
    public void setRemoveTime(Date value);

    /**
     * Getter for <code>cattle.subnet.remove_time</code>.
     */
    @Column(name = "remove_time")
    public Date getRemoveTime();

    /**
     * Setter for <code>cattle.subnet.data</code>.
     */
    public void setData(Map<String,Object> value);

    /**
     * Getter for <code>cattle.subnet.data</code>.
     */
    @Column(name = "data", length = 16777215)
    public Map<String,Object> getData();

    /**
     * Setter for <code>cattle.subnet.network_address</code>.
     */
    public void setNetworkAddress(String value);

    /**
     * Getter for <code>cattle.subnet.network_address</code>.
     */
    @Column(name = "network_address", length = 255)
    public String getNetworkAddress();

    /**
     * Setter for <code>cattle.subnet.cidr_size</code>.
     */
    public void setCidrSize(Integer value);

    /**
     * Getter for <code>cattle.subnet.cidr_size</code>.
     */
    @Column(name = "cidr_size", precision = 10)
    public Integer getCidrSize();

    /**
     * Setter for <code>cattle.subnet.start_address</code>.
     */
    public void setStartAddress(String value);

    /**
     * Getter for <code>cattle.subnet.start_address</code>.
     */
    @Column(name = "start_address", length = 255)
    public String getStartAddress();

    /**
     * Setter for <code>cattle.subnet.end_address</code>.
     */
    public void setEndAddress(String value);

    /**
     * Getter for <code>cattle.subnet.end_address</code>.
     */
    @Column(name = "end_address", length = 255)
    public String getEndAddress();

    /**
     * Setter for <code>cattle.subnet.gateway</code>.
     */
    public void setGateway(String value);

    /**
     * Getter for <code>cattle.subnet.gateway</code>.
     */
    @Column(name = "gateway", length = 255)
    public String getGateway();

    /**
     * Setter for <code>cattle.subnet.network_id</code>.
     */
    public void setNetworkId(Long value);

    /**
     * Getter for <code>cattle.subnet.network_id</code>.
     */
    @Column(name = "network_id", precision = 19)
    public Long getNetworkId();

    /**
     * Setter for <code>cattle.subnet.cluster_id</code>.
     */
    public void setClusterId(Long value);

    /**
     * Getter for <code>cattle.subnet.cluster_id</code>.
     */
    @Column(name = "cluster_id", nullable = false, precision = 19)
    public Long getClusterId();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface Subnet
     */
    public void from(io.cattle.platform.core.model.Subnet from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface Subnet
     */
    public <E extends io.cattle.platform.core.model.Subnet> E into(E into);
}
