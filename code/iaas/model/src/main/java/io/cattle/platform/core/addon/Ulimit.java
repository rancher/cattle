package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class Ulimit {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHard() {
        return hard;
    }

    public void setHard(Integer hard) {
        this.hard = hard;
    }

    public Integer getSoft() {
        return soft;
    }

    public void setSoft(Integer soft) {
        this.soft = soft;
    }

    String name;
    Integer hard;
    Integer soft;
}