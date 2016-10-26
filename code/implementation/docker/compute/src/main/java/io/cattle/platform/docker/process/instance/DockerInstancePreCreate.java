package io.cattle.platform.docker.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.storage.dao.DockerStorageDao;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class DockerInstancePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Inject
    ObjectManager objectManager;
    @Inject
    DockerStorageDao dockerStorageDao;
    @Inject
    NetworkService networkService;

    @Override
    public String[] getProcessNames() {
        return new String[]{"instance.create"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.CONTAINER_LIKE.contains(instance.getKind())) {
            return null;
        }

        Image image = objectManager.loadResource(Image.class, instance.getImageId());
        if (image == null) {
            dockerStorageDao.createImageForInstance(instance);
        }

        Map<Object, Object> data = new HashMap<>();

        String mode = networkService.getNetworkMode(instance);
        data.put(DockerInstanceConstants.FIELD_NETWORK_MODE, mode);

        Network network = networkService.resolveNetwork(instance.getAccountId(), mode);
        if (network != null) {
            data.put(InstanceConstants.FIELD_NETWORK_IDS, Arrays.asList(network.getId()));
        }

        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        Object ip = labels.get(SystemLabels.LABEL_REQUESTED_IP);
        if (ip != null) {
            data.put(InstanceConstants.FIELD_REQUESTED_IP_ADDRESS, ip.toString());
        }

        return new HandlerResult(data).withShouldContinue(true);
    }

    @Override
    public int getPriority() {
        return Priority.PRE - 1;
    }

}
