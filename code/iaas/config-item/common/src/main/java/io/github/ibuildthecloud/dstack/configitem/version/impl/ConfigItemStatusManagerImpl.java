package io.github.ibuildthecloud.dstack.configitem.version.impl;

import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;
import io.github.ibuildthecloud.dstack.configitem.version.ConfigItemStatusManager;
import io.github.ibuildthecloud.dstack.configitem.version.dao.ConfigItemStatusDao;

import javax.inject.Inject;

public class ConfigItemStatusManagerImpl implements ConfigItemStatusManager {

    ConfigItemStatusDao configItemStatusDao;

    @Override
    public boolean setApplied(Client client, String itemName, ItemVersion version) {
        return configItemStatusDao.setApplied(client, itemName, version);
    }

    @Override
    public void setLatest(Client client, String itemName, String sourceRevision) {
        configItemStatusDao.setLatest(client, itemName, sourceRevision);
    }

    @Override
    public boolean isAssigned(Client client, String itemName) {
        return configItemStatusDao.isAssigned(client, itemName);
    }

    public ConfigItemStatusDao getConfigItemStatusDao() {
        return configItemStatusDao;
    }

    @Inject
    public void setConfigItemStatusDao(ConfigItemStatusDao configItemStatusDao) {
        this.configItemStatusDao = configItemStatusDao;
    }

    @Override
    public void setItemSourceVersion(String name, String sourceRevision) {
        configItemStatusDao.setItemSourceVersion(name, sourceRevision);
    }

}
