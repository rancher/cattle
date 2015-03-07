package io.cattle.platform.liquibase;

import javax.annotation.PostConstruct;

import liquibase.servicelocator.ServiceLocator;

public class JarInJarServiceLocator extends ServiceLocator {

    public JarInJarServiceLocator() {
        super(new JarInJarPackageScanner());
    }

    @PostConstruct
    public void init() {
        ServiceLocator.setInstance(this);
    }

}
