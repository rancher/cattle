package io.cattle.platform.docker.api.model;

public class ContainerExecOutput {

    String url;
    String token;

    public ContainerExecOutput() {
    }

    public ContainerExecOutput(String url, String token) {
        super();
        this.url = url;
        this.token = token;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

}
