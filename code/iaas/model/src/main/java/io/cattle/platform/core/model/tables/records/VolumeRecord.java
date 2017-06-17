/*
 * This file is generated by jOOQ.
*/
package io.cattle.platform.core.model.tables.records;


import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.tables.VolumeTable;
import io.cattle.platform.db.jooq.utils.TableRecordJaxb;

import java.util.Date;
import java.util.Map;

import javax.annotation.Generated;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


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
@Table(name = "volume", schema = "cattle")
public class VolumeRecord extends UpdatableRecordImpl<VolumeRecord> implements TableRecordJaxb, Volume {

    private static final long serialVersionUID = -390296025;

    /**
     * Setter for <code>cattle.volume.id</code>.
     */
    @Override
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>cattle.volume.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    @Override
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>cattle.volume.name</code>.
     */
    @Override
    public void setName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>cattle.volume.name</code>.
     */
    @Column(name = "name", length = 255)
    @Override
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>cattle.volume.account_id</code>.
     */
    @Override
    public void setAccountId(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>cattle.volume.account_id</code>.
     */
    @Column(name = "account_id", precision = 19)
    @Override
    public Long getAccountId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>cattle.volume.kind</code>.
     */
    @Override
    public void setKind(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>cattle.volume.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    @Override
    public String getKind() {
        return (String) get(3);
    }

    /**
     * Setter for <code>cattle.volume.uuid</code>.
     */
    @Override
    public void setUuid(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>cattle.volume.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    @Override
    public String getUuid() {
        return (String) get(4);
    }

    /**
     * Setter for <code>cattle.volume.description</code>.
     */
    @Override
    public void setDescription(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>cattle.volume.description</code>.
     */
    @Column(name = "description", length = 1024)
    @Override
    public String getDescription() {
        return (String) get(5);
    }

    /**
     * Setter for <code>cattle.volume.state</code>.
     */
    @Override
    public void setState(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>cattle.volume.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    @Override
    public String getState() {
        return (String) get(6);
    }

    /**
     * Setter for <code>cattle.volume.created</code>.
     */
    @Override
    public void setCreated(Date value) {
        set(7, value);
    }

    /**
     * Getter for <code>cattle.volume.created</code>.
     */
    @Column(name = "created")
    @Override
    public Date getCreated() {
        return (Date) get(7);
    }

    /**
     * Setter for <code>cattle.volume.removed</code>.
     */
    @Override
    public void setRemoved(Date value) {
        set(8, value);
    }

    /**
     * Getter for <code>cattle.volume.removed</code>.
     */
    @Column(name = "removed")
    @Override
    public Date getRemoved() {
        return (Date) get(8);
    }

    /**
     * Setter for <code>cattle.volume.remove_time</code>.
     */
    @Override
    public void setRemoveTime(Date value) {
        set(9, value);
    }

    /**
     * Getter for <code>cattle.volume.remove_time</code>.
     */
    @Column(name = "remove_time")
    @Override
    public Date getRemoveTime() {
        return (Date) get(9);
    }

    /**
     * Setter for <code>cattle.volume.data</code>.
     */
    @Override
    public void setData(Map<String,Object> value) {
        set(10, value);
    }

    /**
     * Getter for <code>cattle.volume.data</code>.
     */
    @Column(name = "data", length = 16777215)
    @Override
    public Map<String,Object> getData() {
        return (Map<String,Object>) get(10);
    }

    /**
     * Setter for <code>cattle.volume.physical_size_mb</code>.
     */
    @Override
    public void setPhysicalSizeMb(Long value) {
        set(11, value);
    }

    /**
     * Getter for <code>cattle.volume.physical_size_mb</code>.
     */
    @Column(name = "physical_size_mb", precision = 19)
    @Override
    public Long getPhysicalSizeMb() {
        return (Long) get(11);
    }

    /**
     * Setter for <code>cattle.volume.virtual_size_mb</code>.
     */
    @Override
    public void setVirtualSizeMb(Long value) {
        set(12, value);
    }

    /**
     * Getter for <code>cattle.volume.virtual_size_mb</code>.
     */
    @Column(name = "virtual_size_mb", precision = 19)
    @Override
    public Long getVirtualSizeMb() {
        return (Long) get(12);
    }

    /**
     * Setter for <code>cattle.volume.format</code>.
     */
    @Override
    public void setFormat(String value) {
        set(13, value);
    }

    /**
     * Getter for <code>cattle.volume.format</code>.
     */
    @Column(name = "format", length = 255)
    @Override
    public String getFormat() {
        return (String) get(13);
    }

    /**
     * Setter for <code>cattle.volume.uri</code>.
     */
    @Override
    public void setUri(String value) {
        set(14, value);
    }

    /**
     * Getter for <code>cattle.volume.uri</code>.
     */
    @Column(name = "uri", length = 255)
    @Override
    public String getUri() {
        return (String) get(14);
    }

    /**
     * Setter for <code>cattle.volume.external_id</code>.
     */
    @Override
    public void setExternalId(String value) {
        set(15, value);
    }

    /**
     * Getter for <code>cattle.volume.external_id</code>.
     */
    @Column(name = "external_id", length = 128)
    @Override
    public String getExternalId() {
        return (String) get(15);
    }

    /**
     * Setter for <code>cattle.volume.access_mode</code>.
     */
    @Override
    public void setAccessMode(String value) {
        set(16, value);
    }

    /**
     * Getter for <code>cattle.volume.access_mode</code>.
     */
    @Column(name = "access_mode", length = 255)
    @Override
    public String getAccessMode() {
        return (String) get(16);
    }

    /**
     * Setter for <code>cattle.volume.host_id</code>.
     */
    @Override
    public void setHostId(Long value) {
        set(17, value);
    }

    /**
     * Getter for <code>cattle.volume.host_id</code>.
     */
    @Column(name = "host_id", precision = 19)
    @Override
    public Long getHostId() {
        return (Long) get(17);
    }

    /**
     * Setter for <code>cattle.volume.deployment_unit_id</code>.
     */
    @Override
    public void setDeploymentUnitId(Long value) {
        set(18, value);
    }

    /**
     * Getter for <code>cattle.volume.deployment_unit_id</code>.
     */
    @Column(name = "deployment_unit_id", precision = 19)
    @Override
    public Long getDeploymentUnitId() {
        return (Long) get(18);
    }

    /**
     * Setter for <code>cattle.volume.environment_id</code>.
     */
    @Override
    public void setStackId(Long value) {
        set(19, value);
    }

    /**
     * Getter for <code>cattle.volume.environment_id</code>.
     */
    @Column(name = "environment_id", precision = 19)
    @Override
    public Long getStackId() {
        return (Long) get(19);
    }

    /**
     * Setter for <code>cattle.volume.volume_template_id</code>.
     */
    @Override
    public void setVolumeTemplateId(Long value) {
        set(20, value);
    }

    /**
     * Getter for <code>cattle.volume.volume_template_id</code>.
     */
    @Column(name = "volume_template_id", precision = 19)
    @Override
    public Long getVolumeTemplateId() {
        return (Long) get(20);
    }

    /**
     * Setter for <code>cattle.volume.storage_driver_id</code>.
     */
    @Override
    public void setStorageDriverId(Long value) {
        set(21, value);
    }

    /**
     * Getter for <code>cattle.volume.storage_driver_id</code>.
     */
    @Column(name = "storage_driver_id", precision = 19)
    @Override
    public Long getStorageDriverId() {
        return (Long) get(21);
    }

    /**
     * Setter for <code>cattle.volume.size_mb</code>.
     */
    @Override
    public void setSizeMb(Long value) {
        set(22, value);
    }

    /**
     * Getter for <code>cattle.volume.size_mb</code>.
     */
    @Column(name = "size_mb", precision = 19)
    @Override
    public Long getSizeMb() {
        return (Long) get(22);
    }

    /**
     * Setter for <code>cattle.volume.storage_pool_id</code>.
     */
    @Override
    public void setStoragePoolId(Long value) {
        set(23, value);
    }

    /**
     * Getter for <code>cattle.volume.storage_pool_id</code>.
     */
    @Column(name = "storage_pool_id", precision = 19)
    @Override
    public Long getStoragePoolId() {
        return (Long) get(23);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void from(Volume from) {
        setId(from.getId());
        setName(from.getName());
        setAccountId(from.getAccountId());
        setKind(from.getKind());
        setUuid(from.getUuid());
        setDescription(from.getDescription());
        setState(from.getState());
        setCreated(from.getCreated());
        setRemoved(from.getRemoved());
        setRemoveTime(from.getRemoveTime());
        setData(from.getData());
        setPhysicalSizeMb(from.getPhysicalSizeMb());
        setVirtualSizeMb(from.getVirtualSizeMb());
        setFormat(from.getFormat());
        setUri(from.getUri());
        setExternalId(from.getExternalId());
        setAccessMode(from.getAccessMode());
        setHostId(from.getHostId());
        setDeploymentUnitId(from.getDeploymentUnitId());
        setStackId(from.getStackId());
        setVolumeTemplateId(from.getVolumeTemplateId());
        setStorageDriverId(from.getStorageDriverId());
        setSizeMb(from.getSizeMb());
        setStoragePoolId(from.getStoragePoolId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Volume> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached VolumeRecord
     */
    public VolumeRecord() {
        super(VolumeTable.VOLUME);
    }

    /**
     * Create a detached, initialised VolumeRecord
     */
    public VolumeRecord(Long id, String name, Long accountId, String kind, String uuid, String description, String state, Date created, Date removed, Date removeTime, Map<String,Object> data, Long physicalSizeMb, Long virtualSizeMb, String format, String uri, String externalId, String accessMode, Long hostId, Long deploymentUnitId, Long environmentId, Long volumeTemplateId, Long storageDriverId, Long sizeMb, Long storagePoolId) {
        super(VolumeTable.VOLUME);

        set(0, id);
        set(1, name);
        set(2, accountId);
        set(3, kind);
        set(4, uuid);
        set(5, description);
        set(6, state);
        set(7, created);
        set(8, removed);
        set(9, removeTime);
        set(10, data);
        set(11, physicalSizeMb);
        set(12, virtualSizeMb);
        set(13, format);
        set(14, uri);
        set(15, externalId);
        set(16, accessMode);
        set(17, hostId);
        set(18, deploymentUnitId);
        set(19, environmentId);
        set(20, volumeTemplateId);
        set(21, storageDriverId);
        set(22, sizeMb);
        set(23, storagePoolId);
    }
}
