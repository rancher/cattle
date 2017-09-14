package io.cattle.platform.api.hostapi;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = "hostApiProxyToken")
public class HostApiProxyTokenImpl implements HostApiProxyToken {

    private String reportedUuid;
    private String token;
    private String url;

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getReportedUuid() {
        return reportedUuid;
    }

    public void setReportedUuid(String reportedUuid) {
        this.reportedUuid = reportedUuid;
    }

    @Override
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
