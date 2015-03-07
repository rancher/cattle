package io.cattle.platform.extension.impl;

import io.cattle.platform.extension.ExtensionImplementation;
import io.cattle.platform.extension.ExtensionPoint;

import java.util.List;

public class ExtensionPointImpl implements ExtensionPoint {

    String name;
    List<ExtensionImplementation> implementations;
    String listSetting;
    String excludeSetting;
    String includeSetting;

    public ExtensionPointImpl(String name, List<ExtensionImplementation> implementations, String listSetting, String excludeSetting, String includeSetting) {
        super();
        this.name = name;
        this.implementations = implementations;
        this.listSetting = listSetting;
        this.excludeSetting = excludeSetting;
        this.includeSetting = includeSetting;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<ExtensionImplementation> getImplementations() {
        return implementations;
    }

    @Override
    public String getListSetting() {
        return listSetting;
    }

    @Override
    public String getExcludeSetting() {
        return excludeSetting;
    }

    @Override
    public String getIncludeSetting() {
        return includeSetting;
    }

}
