package io.cattle.platform.api.auth.impl;

public class NoPolicyOptions implements PolicyOptions {

    @Override
    public boolean isOption(String optionName) {
        return false;
    }

    @Override
    public String getOption(String optionName) {
        return null;
    }

    @Override
    public void setOption(String name, String value) {
    }

}
