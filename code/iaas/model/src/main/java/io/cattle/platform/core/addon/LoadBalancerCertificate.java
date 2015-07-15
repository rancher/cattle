package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class LoadBalancerCertificate {
    long certificateId;
    boolean isDefault;

    public LoadBalancerCertificate() {

    }

    public LoadBalancerCertificate(long certificateId, boolean isDefault) {
        super();
        this.certificateId = certificateId;
        this.isDefault = isDefault;
    }
    public long getCertificateId() {
        return certificateId;
    }
    public void setCertificateId(long certificateId) {
        this.certificateId = certificateId;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (certificateId ^ (certificateId >>> 32));
        result = prime * result + (isDefault ? 1231 : 1237);
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
        LoadBalancerCertificate other = (LoadBalancerCertificate) obj;
        if (certificateId != other.certificateId)
            return false;
        if (isDefault != other.isDefault)
            return false;
        return true;
    }
}
