package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class LoadBalancerCookieStickinessPolicy {

    String name;
    String cookie;
    String domain;
    Boolean indirect = false;
    Boolean nocache = false;
    Boolean postonly = false;
    Mode mode;

    public LoadBalancerCookieStickinessPolicy() {
    }

    public enum Mode {
        rewrite,
        insert,
        prefix;
    }

    @Field(required = false, nullable = true)
    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getIndirect() {
        return indirect;
    }

    public void setIndirect(Boolean indirect) {
        this.indirect = indirect;
    }

    public Boolean getNocache() {
        return nocache;
    }

    public void setNocache(Boolean nocache) {
        this.nocache = nocache;
    }

    public Boolean getPostonly() {
        return postonly;
    }

    public void setPostonly(Boolean postonly) {
        this.postonly = postonly;
    }

    @Field(required = false, nullable = true, defaultValue = "insert")
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
}