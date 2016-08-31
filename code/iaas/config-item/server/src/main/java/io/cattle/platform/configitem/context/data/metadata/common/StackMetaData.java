package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao.Version;
import io.cattle.platform.configitem.context.data.metadata.version1.StackMetaDataVersion1;
import io.cattle.platform.configitem.context.data.metadata.version2.StackMetaDataVersion2;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Stack;

import java.util.ArrayList;
import java.util.List;

public class StackMetaData {
    private long id;

    protected String environment_name;
    protected String environment_uuid;
    protected String name;
    protected String uuid;
    protected List<ServiceMetaData> services = new ArrayList<>();

    public StackMetaData(Stack stack, Account account) {
        this.name = stack.getName();
        this.uuid = stack.getUuid();
        this.environment_name = account.getName();
        this.id = stack.getId();
        this.environment_uuid = account.getUuid();
    }

    protected StackMetaData(StackMetaData that) {
        this.environment_name = that.environment_name;
        this.name = that.name;
        this.uuid = that.uuid;
        this.services = that.services;
        this.id = that.id;
        this.environment_uuid = that.environment_uuid;
    }

    public String getEnvironment_uuid() {
        return environment_uuid;
    }

    public void setEnvironment_uuid (String environment_uuid) {
        this.environment_uuid = environment_uuid;
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

    public long getId() {
        return id;
    }

    public void setServicesObj(List<ServiceMetaData> services) {
        this.services = services;
    }

    public static StackMetaData getStackMetaData(StackMetaData stackData, Version version) {
        if (version == MetaDataInfoDao.Version.version1) {
            return new StackMetaDataVersion1(stackData);
        } else {
            return new StackMetaDataVersion2(stackData);
        }
    }
}
