package io.cattle.platform.docker.process.nic;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.docker.process.dao.DockerComputeDao;
import io.cattle.platform.docker.process.util.DockerProcessUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;

public class DockerNicDeactivate extends AbstractObjectProcessHandler {

    DockerComputeDao computeDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.deactivate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic) state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, nic.getInstanceId());

        if (instance != null && InstanceConstants.KIND_CONTAINER.equals(instance.getKind())) {
            String ip = DockerProcessUtils.getDockerIp(instance);
            IpAddress ipAddress = computeDao.getDockerIp(ip, instance);

            if (ipAddress != null) {
                getObjectProcessManager().executeStandardProcess(StandardProcess.DEACTIVATE, ipAddress, null);
            }
        }

        return null;
    }

    public DockerComputeDao getComputeDao() {
        return computeDao;
    }

    @Inject
    public void setComputeDao(DockerComputeDao computeDao) {
        this.computeDao = computeDao;
    }

}
