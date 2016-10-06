package io.cattle.platform.process.host;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.MachineConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class HostCreateToProvision extends AbstractObjectProcessLogic implements ProcessPostListener {

    @Inject
    HostDao hostDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getProcessNames() {
        return new String[] {HostConstants.PROCESS_CREATE};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Host host = (Host)state.getResource();
        String driver = getDriver(host);
        if (StringUtils.isBlank(driver)) {
            return new HandlerResult().withChainProcessName(HostConstants.PROCESS_ACTIVATE);
        }

        PhysicalHost phyHost = objectManager.loadResource(PhysicalHost.class, host.getPhysicalHostId());
        if (phyHost == null) {
            phyHost = hostDao.createMachineForHost(host);
        }

        return new HandlerResult(MachineConstants.FIELD_DRIVER, driver).withChainProcessName(HostConstants.PROCESS_PROVISION);
    }

    public static String getDriver(Object obj) {
        Map<String, Object> fields = DataUtils.getFields(obj);
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (StringUtils.endsWithIgnoreCase(field.getKey(), MachineConstants.CONFIG_FIELD_SUFFIX) && field.getValue() != null) {
                return StringUtils.removeEndIgnoreCase(field.getKey(), MachineConstants.CONFIG_FIELD_SUFFIX);
            }
        }
        return null;
    }

}
