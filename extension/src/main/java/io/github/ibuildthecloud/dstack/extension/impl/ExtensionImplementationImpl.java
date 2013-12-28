package io.github.ibuildthecloud.dstack.extension.impl;

import io.github.ibuildthecloud.dstack.extension.ExtensionImplementation;

public class ExtensionImplementationImpl implements ExtensionImplementation {

    String name, className;

    public ExtensionImplementationImpl(String name, String className) {
        super();
        this.name = name;
        this.className = className;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return className;
    }

}
