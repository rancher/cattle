package io.cattle.platform.process.instance;

import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;

public class PodRemove extends PodHandler
{
    public PodRemove(EventService eventService, ObjectManager objectManager, ObjectProcessManager objectProcessManager, ObjectMetaDataManager objectMetaDataManager, DeploymentSyncFactory syncFactory, ObjectSerializer objectSerializer) {
        super(eventService, objectManager, objectProcessManager, objectMetaDataManager, syncFactory, objectSerializer);
    }
}
