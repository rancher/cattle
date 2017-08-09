package io.cattle.platform.configitem.context.data.metadata.common;

public class CredentialMetaData {

    String url;
    String public_value;
    String secret_value;
    // helper field needed by metadata service to process object
    String metadata_kind;

    public CredentialMetaData(String url, String public_value, String secret_value) {
        super();
        this.url = url;
        this.public_value = public_value;
        this.secret_value = secret_value;
        metadata_kind = "credential";
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublic_value() {
        return public_value;
    }

    public void setPublic_value(String public_value) {
        this.public_value = public_value;
    }

    public String getSecret_value() {
        return secret_value;
    }

    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }

    public void setSecret_value(String secret_value) {
        this.secret_value = secret_value;
    }

}
