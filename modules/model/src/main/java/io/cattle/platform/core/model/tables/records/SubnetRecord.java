/*
 * This file is generated by jOOQ.
*/
package io.cattle.platform.core.model.tables.records;


import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.SubnetTable;
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

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record17;
import org.jooq.Row17;
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
@Table(name = "subnet", schema = "cattle")
public class SubnetRecord extends UpdatableRecordImpl<SubnetRecord> implements TableRecordJaxb, Record17<Long, String, String, String, String, String, Date, Date, Date, Map<String,Object>, String, Integer, String, String, String, Long, Long>, Subnet {

    private static final long serialVersionUID = -120830156;

    /**
     * Setter for <code>cattle.subnet.id</code>.
     */
    @Override
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>cattle.subnet.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    @Override
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>cattle.subnet.name</code>.
     */
    @Override
    public void setName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>cattle.subnet.name</code>.
     */
    @Column(name = "name", length = 255)
    @Override
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>cattle.subnet.kind</code>.
     */
    @Override
    public void setKind(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>cattle.subnet.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    @Override
    public String getKind() {
        return (String) get(2);
    }

    /**
     * Setter for <code>cattle.subnet.uuid</code>.
     */
    @Override
    public void setUuid(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>cattle.subnet.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    @Override
    public String getUuid() {
        return (String) get(3);
    }

    /**
     * Setter for <code>cattle.subnet.description</code>.
     */
    @Override
    public void setDescription(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>cattle.subnet.description</code>.
     */
    @Column(name = "description", length = 1024)
    @Override
    public String getDescription() {
        return (String) get(4);
    }

    /**
     * Setter for <code>cattle.subnet.state</code>.
     */
    @Override
    public void setState(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>cattle.subnet.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    @Override
    public String getState() {
        return (String) get(5);
    }

    /**
     * Setter for <code>cattle.subnet.created</code>.
     */
    @Override
    public void setCreated(Date value) {
        set(6, value);
    }

    /**
     * Getter for <code>cattle.subnet.created</code>.
     */
    @Column(name = "created")
    @Override
    public Date getCreated() {
        return (Date) get(6);
    }

    /**
     * Setter for <code>cattle.subnet.removed</code>.
     */
    @Override
    public void setRemoved(Date value) {
        set(7, value);
    }

    /**
     * Getter for <code>cattle.subnet.removed</code>.
     */
    @Column(name = "removed")
    @Override
    public Date getRemoved() {
        return (Date) get(7);
    }

    /**
     * Setter for <code>cattle.subnet.remove_time</code>.
     */
    @Override
    public void setRemoveTime(Date value) {
        set(8, value);
    }

    /**
     * Getter for <code>cattle.subnet.remove_time</code>.
     */
    @Column(name = "remove_time")
    @Override
    public Date getRemoveTime() {
        return (Date) get(8);
    }

    /**
     * Setter for <code>cattle.subnet.data</code>.
     */
    @Override
    public void setData(Map<String,Object> value) {
        set(9, value);
    }

    /**
     * Getter for <code>cattle.subnet.data</code>.
     */
    @Column(name = "data", length = 16777215)
    @Override
    public Map<String,Object> getData() {
        return (Map<String,Object>) get(9);
    }

    /**
     * Setter for <code>cattle.subnet.network_address</code>.
     */
    @Override
    public void setNetworkAddress(String value) {
        set(10, value);
    }

    /**
     * Getter for <code>cattle.subnet.network_address</code>.
     */
    @Column(name = "network_address", length = 255)
    @Override
    public String getNetworkAddress() {
        return (String) get(10);
    }

    /**
     * Setter for <code>cattle.subnet.cidr_size</code>.
     */
    @Override
    public void setCidrSize(Integer value) {
        set(11, value);
    }

    /**
     * Getter for <code>cattle.subnet.cidr_size</code>.
     */
    @Column(name = "cidr_size", precision = 10)
    @Override
    public Integer getCidrSize() {
        return (Integer) get(11);
    }

    /**
     * Setter for <code>cattle.subnet.start_address</code>.
     */
    @Override
    public void setStartAddress(String value) {
        set(12, value);
    }

    /**
     * Getter for <code>cattle.subnet.start_address</code>.
     */
    @Column(name = "start_address", length = 255)
    @Override
    public String getStartAddress() {
        return (String) get(12);
    }

    /**
     * Setter for <code>cattle.subnet.end_address</code>.
     */
    @Override
    public void setEndAddress(String value) {
        set(13, value);
    }

    /**
     * Getter for <code>cattle.subnet.end_address</code>.
     */
    @Column(name = "end_address", length = 255)
    @Override
    public String getEndAddress() {
        return (String) get(13);
    }

    /**
     * Setter for <code>cattle.subnet.gateway</code>.
     */
    @Override
    public void setGateway(String value) {
        set(14, value);
    }

    /**
     * Getter for <code>cattle.subnet.gateway</code>.
     */
    @Column(name = "gateway", length = 255)
    @Override
    public String getGateway() {
        return (String) get(14);
    }

    /**
     * Setter for <code>cattle.subnet.network_id</code>.
     */
    @Override
    public void setNetworkId(Long value) {
        set(15, value);
    }

    /**
     * Getter for <code>cattle.subnet.network_id</code>.
     */
    @Column(name = "network_id", precision = 19)
    @Override
    public Long getNetworkId() {
        return (Long) get(15);
    }

    /**
     * Setter for <code>cattle.subnet.cluster_id</code>.
     */
    @Override
    public void setClusterId(Long value) {
        set(16, value);
    }

    /**
     * Getter for <code>cattle.subnet.cluster_id</code>.
     */
    @Column(name = "cluster_id", nullable = false, precision = 19)
    @Override
    public Long getClusterId() {
        return (Long) get(16);
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
    // Record17 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row17<Long, String, String, String, String, String, Date, Date, Date, Map<String,Object>, String, Integer, String, String, String, Long, Long> fieldsRow() {
        return (Row17) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row17<Long, String, String, String, String, String, Date, Date, Date, Map<String,Object>, String, Integer, String, String, String, Long, Long> valuesRow() {
        return (Row17) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field1() {
        return SubnetTable.SUBNET.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field2() {
        return SubnetTable.SUBNET.NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field3() {
        return SubnetTable.SUBNET.KIND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field4() {
        return SubnetTable.SUBNET.UUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field5() {
        return SubnetTable.SUBNET.DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field6() {
        return SubnetTable.SUBNET.STATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Date> field7() {
        return SubnetTable.SUBNET.CREATED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Date> field8() {
        return SubnetTable.SUBNET.REMOVED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Date> field9() {
        return SubnetTable.SUBNET.REMOVE_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Map<String,Object>> field10() {
        return SubnetTable.SUBNET.DATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field11() {
        return SubnetTable.SUBNET.NETWORK_ADDRESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field12() {
        return SubnetTable.SUBNET.CIDR_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field13() {
        return SubnetTable.SUBNET.START_ADDRESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field14() {
        return SubnetTable.SUBNET.END_ADDRESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field15() {
        return SubnetTable.SUBNET.GATEWAY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field16() {
        return SubnetTable.SUBNET.NETWORK_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field17() {
        return SubnetTable.SUBNET.CLUSTER_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value2() {
        return getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value3() {
        return getKind();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value4() {
        return getUuid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value5() {
        return getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value6() {
        return getState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date value7() {
        return getCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date value8() {
        return getRemoved();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date value9() {
        return getRemoveTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String,Object> value10() {
        return getData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value11() {
        return getNetworkAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value12() {
        return getCidrSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value13() {
        return getStartAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value14() {
        return getEndAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value15() {
        return getGateway();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value16() {
        return getNetworkId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value17() {
        return getClusterId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value1(Long value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value2(String value) {
        setName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value3(String value) {
        setKind(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value4(String value) {
        setUuid(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value5(String value) {
        setDescription(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value6(String value) {
        setState(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value7(Date value) {
        setCreated(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value8(Date value) {
        setRemoved(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value9(Date value) {
        setRemoveTime(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value10(Map<String,Object> value) {
        setData(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value11(String value) {
        setNetworkAddress(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value12(Integer value) {
        setCidrSize(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value13(String value) {
        setStartAddress(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value14(String value) {
        setEndAddress(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value15(String value) {
        setGateway(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value16(Long value) {
        setNetworkId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord value17(Long value) {
        setClusterId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnetRecord values(Long value1, String value2, String value3, String value4, String value5, String value6, Date value7, Date value8, Date value9, Map<String,Object> value10, String value11, Integer value12, String value13, String value14, String value15, Long value16, Long value17) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        value16(value16);
        value17(value17);
        return this;
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void from(Subnet from) {
        setId(from.getId());
        setName(from.getName());
        setKind(from.getKind());
        setUuid(from.getUuid());
        setDescription(from.getDescription());
        setState(from.getState());
        setCreated(from.getCreated());
        setRemoved(from.getRemoved());
        setRemoveTime(from.getRemoveTime());
        setData(from.getData());
        setNetworkAddress(from.getNetworkAddress());
        setCidrSize(from.getCidrSize());
        setStartAddress(from.getStartAddress());
        setEndAddress(from.getEndAddress());
        setGateway(from.getGateway());
        setNetworkId(from.getNetworkId());
        setClusterId(from.getClusterId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Subnet> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SubnetRecord
     */
    public SubnetRecord() {
        super(SubnetTable.SUBNET);
    }

    /**
     * Create a detached, initialised SubnetRecord
     */
    public SubnetRecord(Long id, String name, String kind, String uuid, String description, String state, Date created, Date removed, Date removeTime, Map<String,Object> data, String networkAddress, Integer cidrSize, String startAddress, String endAddress, String gateway, Long networkId, Long clusterId) {
        super(SubnetTable.SUBNET);

        set(0, id);
        set(1, name);
        set(2, kind);
        set(3, uuid);
        set(4, description);
        set(5, state);
        set(6, created);
        set(7, removed);
        set(8, removeTime);
        set(9, data);
        set(10, networkAddress);
        set(11, cidrSize);
        set(12, startAddress);
        set(13, endAddress);
        set(14, gateway);
        set(15, networkId);
        set(16, clusterId);
    }
}
