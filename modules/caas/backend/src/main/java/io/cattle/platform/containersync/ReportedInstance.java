package io.cattle.platform.containersync;

import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

public class ReportedInstance {

    String uuid;
    String externalId;
    String state;

    public ReportedInstance(Map<String, Object> resource) {
        super();
        uuid = DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.UUID_FIELD).as(String.class);
        externalId = DataAccessor.fromMap(resource).withKey("dockerId").as(String.class);
        state = DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.STATE_FIELD).as(String.class);
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
