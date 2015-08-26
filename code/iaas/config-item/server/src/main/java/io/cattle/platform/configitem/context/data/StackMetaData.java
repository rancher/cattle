package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Environment;

import java.util.ArrayList;
import java.util.List;

public class StackMetaData {
    private String environment_name;
    String name;
    List<String> services = new ArrayList<>();

    public StackMetaData(Environment stack, Account account, List<String> services) {
        this.name = stack.getName();
        this.environment_name = account.getName();
        this.services = services;
    }

    public String getEnvironment_name() {
        return environment_name;
    }

    public String getName() {
        return name;
    }

    public List<String> getServices() {
        return services;
    }

}
