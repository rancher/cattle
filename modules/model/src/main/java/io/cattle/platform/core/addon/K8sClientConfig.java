package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class K8sClientConfig {

    String bearerToken;
    String address;
    String caCert;

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCaCert() {
        return caCert;
    }

    public void setCaCert(String caCert) {
        this.caCert = caCert;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        K8sClientConfig that = (K8sClientConfig) o;

        if (bearerToken != null ? !bearerToken.equals(that.bearerToken) : that.bearerToken != null) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        return caCert != null ? caCert.equals(that.caCert) : that.caCert == null;
    }

    @Override
    public int hashCode() {
        int result = bearerToken != null ? bearerToken.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (caCert != null ? caCert.hashCode() : 0);
        return result;
    }
}
