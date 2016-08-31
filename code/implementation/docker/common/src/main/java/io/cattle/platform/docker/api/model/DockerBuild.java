package io.cattle.platform.docker.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list=false)
public class DockerBuild {

    String dockerfile;
    String remote;
    String context;
    String tag;
    boolean nocache;
    boolean rm;
    boolean forcerm;

    public String getDockerfile() {
        return dockerfile;
    }

    @Field(nullable = true)
    public void setDockerfile(String dockerfile) {
        this.dockerfile = dockerfile;
    }

    public boolean isNocache() {
        return nocache;
    }

    public void setNocache(boolean nocache) {
        this.nocache = nocache;
    }

    public boolean isRm() {
        return rm;
    }

    public void setRm(boolean rm) {
        this.rm = rm;
    }

    public boolean isForcerm() {
        return forcerm;
    }

    public void setForcerm(boolean forcerm) {
        this.forcerm = forcerm;
    }

    public String getTag() {
        return tag;
    }

    @Field(nullable = true)
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getRemote() {
        return remote;
    }

    @Field(nullable = true)
    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getContext() {
        return context;
    }

    @Field(nullable = true)
    public void setContext(String context) {
        this.context = context;
    }
}
