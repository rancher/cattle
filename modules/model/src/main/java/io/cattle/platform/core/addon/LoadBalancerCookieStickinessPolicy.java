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
        prefix
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cookie == null) ? 0 : cookie.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((indirect == null) ? 0 : indirect.hashCode());
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nocache == null) ? 0 : nocache.hashCode());
        result = prime * result + ((postonly == null) ? 0 : postonly.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LoadBalancerCookieStickinessPolicy other = (LoadBalancerCookieStickinessPolicy) obj;
        if (cookie == null) {
            if (other.cookie != null)
                return false;
        } else if (!cookie.equals(other.cookie))
            return false;
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;
        if (indirect == null) {
            if (other.indirect != null)
                return false;
        } else if (!indirect.equals(other.indirect))
            return false;
        if (!mode.toString().equals(other.mode.toString()))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nocache == null) {
            if (other.nocache != null)
                return false;
        } else if (!nocache.equals(other.nocache))
            return false;
        if (postonly == null) {
            if (other.postonly != null)
                return false;
        } else if (!postonly.equals(other.postonly))
            return false;
        return true;
    }
}