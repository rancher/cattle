package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

/**
 * HAProxy FORMAT : appsession <cookie> len <length> timeout <timeout>
 * [request-learn] [prefix] [mode
 * <path-parameters|query-string>]
 *
 */

@Type(list = false)
public class LoadBalancerAppCookieStickinessPolicy {
    String name;
    String cookie;
    Integer length;
    Boolean prefix = false;
    Boolean requestLearn = false;
    Integer timeout;
    Mode mode;

    enum Mode {
        path_parameters,
        query_string;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Boolean getPrefix() {
        return prefix;
    }

    public void setPrefix(Boolean prefix) {
        this.prefix = prefix;
    }

    public Boolean getRequestLearn() {
        return requestLearn;
    }

    public void setRequestLearn(Boolean requestLearn) {
        this.requestLearn = requestLearn;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
}
