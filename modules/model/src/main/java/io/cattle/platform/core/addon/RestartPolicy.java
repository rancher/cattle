package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Type(list = false)
public class RestartPolicy {

    public static String RESTART_NEVER = "no";
    public static String RESTART_ALWAYS = "always";
    public static String RESTART_ON_FAILURE = "on-failure";

    String name;
    int maximumRetryCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaximumRetryCount() {
        return maximumRetryCount;
    }

    public void setMaximumRetryCount(int maximumRetryCount) {
        this.maximumRetryCount = maximumRetryCount;
    }

    @Field(include = false)
    @JsonIgnore
    public boolean isNever() {
        return RESTART_NEVER.equals(name) || StringUtils.isBlank(name);
    }

    @Field(include = false)
    @JsonIgnore
    public boolean isAlways() {
        return RESTART_ALWAYS.equals(name);
    }

    @Field(include = false)
    @JsonIgnore
    public boolean isOnFailure() {
        return RESTART_ON_FAILURE.equals(name);
    }

}