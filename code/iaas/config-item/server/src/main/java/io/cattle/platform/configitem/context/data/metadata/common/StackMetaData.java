package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Environment;

import java.util.ArrayList;
import java.util.List;

public class StackMetaData {

    protected String environment_name;
    protected String name;
    protected String uuid;
    protected List<ServiceMetaData> services = new ArrayList<>();

    public StackMetaData(Environment stack, Account account, List<ServiceMetaData> services) {
        this.name = stack.getName();
        this.uuid = stack.getUuid();
        this.environment_name = account.getName();
        this.services = services;
    }

    public StackMetaData(StackMetaData that) {
        this.environment_name = that.environment_name;
        this.name = that.name;
        this.uuid = that.uuid;
        this.services = that.services;
    }

    public String getEnvironment_name() {
        return environment_name;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setEnvironment_name(String environment_name) {
        this.environment_name = environment_name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
