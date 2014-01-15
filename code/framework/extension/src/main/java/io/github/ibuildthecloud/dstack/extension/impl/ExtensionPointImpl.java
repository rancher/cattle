package io.github.ibuildthecloud.dstack.extension.impl;

import java.util.List;

import io.github.ibuildthecloud.dstack.extension.ExtensionImplementation;
import io.github.ibuildthecloud.dstack.extension.ExtensionPoint;

public class ExtensionPointImpl implements ExtensionPoint {

    String name;
    List<ExtensionImplementation> implementations;
    String listSetting;
    String excludeSetting;
    String includeSetting;

    public ExtensionPointImpl(String name, List<ExtensionImplementation> implementations, String listSetting,
            String excludeSetting, String includeSetting) {
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
