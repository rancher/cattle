package io.cattle.platform.containersync;

import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class ReportedInstance {

    String uuid;
    String externalId;
    String state;
    String image;

    public ReportedInstance(Map<String, Object> resource) {
        super();
        uuid = DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.UUID_FIELD).as(String.class);
        externalId = DataAccessor.fromMap(resource).withKey("dockerId").as(String.class);
        state = DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.STATE_FIELD).as(String.class);
        image = DataAccessor.fromMap(resource).withKey("image").as(String.class);
    }

    public String getUuid() {
        return uuid;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getState() {
        return state;
    }

    public String getImage() {
        return image;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
