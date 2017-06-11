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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((dockerfile == null) ? 0 : dockerfile.hashCode());
        result = prime * result + (forcerm ? 1231 : 1237);
        result = prime * result + (nocache ? 1231 : 1237);
        result = prime * result + ((remote == null) ? 0 : remote.hashCode());
        result = prime * result + (rm ? 1231 : 1237);
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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
        DockerBuild other = (DockerBuild) obj;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (dockerfile == null) {
            if (other.dockerfile != null)
                return false;
        } else if (!dockerfile.equals(other.dockerfile))
            return false;
        if (forcerm != other.forcerm)
            return false;
        if (nocache != other.nocache)
            return false;
        if (remote == null) {
            if (other.remote != null)
                return false;
        } else if (!remote.equals(other.remote))
            return false;
        if (rm != other.rm)
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }

}
