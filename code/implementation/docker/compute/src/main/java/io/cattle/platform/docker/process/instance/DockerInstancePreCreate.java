package io.cattle.platform.docker.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.Arrays;
import javax.inject.Inject;

public class DockerInstancePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    NetworkDao networkDao;

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

        String mode = DataAccessor.fieldString(instance, DockerInstanceConstants.FIELD_NETWORK_MODE);
        String kind = DockerNetworkConstants.MODE_TO_KIND.get(mode);

        if (mode == null || kind == null) {
            return null;
        }

        Network network = networkDao.getNetworkForObject(instance, kind);
        if (network == null) {
            return null;
        }

        return new HandlerResult(InstanceConstants.FIELD_NETWORK_IDS, Arrays.asList(network.getId())).withShouldContinue(true);
    }
}
