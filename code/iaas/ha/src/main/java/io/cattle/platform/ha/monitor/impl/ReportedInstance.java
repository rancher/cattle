package io.cattle.platform.ha.monitor.impl;

import io.cattle.platform.ha.monitor.model.KnownInstance;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

class ReportedInstance {

    String uuid;
    String externalId;
    String state;
    String image;
    Long created;
    Map<String, String> labels;
    KnownInstance instance;

    protected ReportedInstance() {
        super();
        labels = new HashMap<String, String>();
    }

    protected ReportedInstance(Map<String, Object> resource) {
        super();
        uuid = DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.UUID_FIELD).as(String.class);
        externalId = DataAccessor.fromMap(resource).withKey("dockerId").as(String.class);
        state = DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.STATE_FIELD).as(String.class);
        image = DataAccessor.fromMap(resource).withKey("image").as(String.class);
        created = DataAccessor.fromMap(resource).withKey("created").as(Long.class);
        labels = CollectionUtils.toMap(DataAccessor.fromMap(resource).withKey("labels").get());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public KnownInstance getInstance() {
        return instance;
    }

    public void setInstance(KnownInstance instance) {
        this.instance = instance;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
