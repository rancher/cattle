package io.cattle.platform.api.auth.impl;

public interface PolicyOptions {

    boolean isOption(String optionName);

    String getOption(String optionName);

    void setOption(String name, String value);

}
