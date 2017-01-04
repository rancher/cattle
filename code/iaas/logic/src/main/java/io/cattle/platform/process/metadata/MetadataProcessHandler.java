package io.cattle.platform.process.metadata;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.service.AgentMetadataService;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringListProperty;

@Named
public class MetadataProcessHandler extends AbstractObjectProcessLogic implements ProcessPostListener {

    private static DynamicStringListProperty PROCESSES = ArchaiusUtil.getList("metadata.increment.processes");
    private static Logger log = LoggerFactory.getLogger(MetadataProcessHandler.class);

    @Inject
    AgentInstanceDao agentInstanceDao;
    @Inject
    ConfigItemStatusManager statusManager;
    @Inject
    AgentMetadataService agentMetadata;
    @Inject
    AccountDao accountDao;

    @Override
    public String[] getProcessNames() {
        List<String> processes = PROCESSES.get();
        return processes.toArray(new String[processes.size()]);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object obj = getAccountObject(state.getResource());
        Object accountId;
        if (obj instanceof Account) {
            accountId = ObjectUtils.getId(obj);
        } else {
            accountId = ObjectUtils.getAccountId(obj);
        }

        if (!(accountId instanceof Long)) {
            log.error("Failed to find account id for {}:{}", ObjectUtils.getKind(obj), ObjectUtils.getId(obj));
            return null;
        }
        List<Long> accountIds = new ArrayList<>();
        accountIds.add((Long) accountId);
        accountIds.addAll(accountDao.getLinkedAccounts((Long) accountId));
        for (long id : accountIds) {
            agentMetadata.updateMetadata(id);
        }

        return null;
    }

    protected Object getAccountObject(Object obj) {
        if (obj instanceof HostIpAddressMap) {
            return objectManager.loadResource(IpAddress.class, ((HostIpAddressMap) obj).getIpAddressId());
        } else if (obj instanceof InstanceHostMap) {
            return objectManager.loadResource(Instance.class, ((InstanceHostMap) obj).getInstanceId());
        } else if (obj instanceof ServiceConsumeMap) {
            return objectManager.loadResource(Service.class, ((ServiceConsumeMap) obj).getServiceId());
        } else if (obj instanceof ServiceExposeMap) {
            return objectManager.loadResource(Service.class, ((ServiceExposeMap) obj).getServiceId());
        }
        return obj;
    }

}
