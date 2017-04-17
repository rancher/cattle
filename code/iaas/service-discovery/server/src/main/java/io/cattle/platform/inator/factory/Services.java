package io.cattle.platform.inator.factory;

import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Services {

    @Inject
    public ObjectManager objectManager;
    @Inject
    public ServiceDao serviceDao;
    @Inject
    public ObjectProcessManager processManager;
    @Inject
    public ObjectMetaDataManager metadataManager;
    @Inject
    public JsonMapper jsonMapper;
    @Inject
    public ServiceDiscoveryService sdService;
    @Inject
    public NetworkService networkService;

}
