package io.cattle.platform.docker.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = "serviceProxy", pluralName="serviceProxies")
public interface ServiceProxy {

    public enum Scheme {
       http,
       https,
    }

    @Field(defaultValue = "80", min = 0)
    Integer getPort();

    @Field(defaultValue = "http")
    Scheme getScheme();

    @Field(required = true, nullable = false, minLength=1)
    String getService();

    String getToken();

    String getUrl();

}
