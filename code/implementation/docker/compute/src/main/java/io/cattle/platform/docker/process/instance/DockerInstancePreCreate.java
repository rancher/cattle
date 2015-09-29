package io.cattle.platform.docker.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.docker.storage.dao.DockerStorageDao;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class DockerInstancePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    NetworkDao networkDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    DockerStorageDao dockerStorageDao;

    @Override
    public String[] getProcessNames() {
        return new String[]{"instance.create"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.KIND_CONTAINER.equals(instance.getKind())) {
            return null;
        }

        Image image = objectManager.loadResource(Image.class, instance.getImageId());
        if (image == null) {
            dockerStorageDao.createImageForInstance(instance);
        }
        String mode = DataAccessor.fieldString(instance, DockerInstanceConstants.FIELD_NETWORK_MODE);
        String kind = DockerNetworkConstants.MODE_TO_KIND.get(mode);

        if (mode == null || kind == null) {
            return null;
        }

        Network network = networkDao.getNetworkForObject(instance, kind);
        if (network == null) {
            return null;
        }

        Map<Object, Object> data = new HashMap<>();
        data.put(InstanceConstants.FIELD_NETWORK_IDS, Arrays.asList(network.getId()));

        if (DockerNetworkConstants.NETWORK_MODE_MANAGED.equals(mode) && network != null) {
            Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
            Object ip = labels.get(SystemLabels.LABEL_REQUESTED_IP);
            if (ip != null) {
                data.put(InstanceConstants.FIELD_REQUESTED_IP_ADDRESS, ip.toString());
            }
        }

        return new HandlerResult(data).withShouldContinue(true);
    }

}
