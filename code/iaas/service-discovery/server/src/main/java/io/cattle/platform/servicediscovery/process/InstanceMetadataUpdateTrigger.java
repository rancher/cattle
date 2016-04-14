package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.EnvironmentTable.*;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;

public class InstanceMetadataUpdateTrigger extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Inject
    ServiceDao serviceDao;

    @Inject
    InstanceDao instanceDao;
 
    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate",
                "nic.deactivate",
                "serviceconsumemap.create",
                "serviceconsumemap.remove",
                "serviceconsumemap.update",
                "serviceexposemap.create",
                "serviceexposemap.remove",
                "service.activate",
                "service.deactivate",
                "service.remove",
                "service.udpate",
                "host.create",
                "host.remove",
                "instance.updatehealthy",
                "instance.updateunhealthy",
                "instance.remove",
                "account.update" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Pair<Long, List<Object>> accountAndObject = getAccountAndObject(state);
        if (accountAndObject != null) {
            for (Object object : accountAndObject.getRight()) {
                serviceDao.incrementMetadataRevision(accountAndObject.getLeft(), object);
            }
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    private Pair<Long, List<Object>> getAccountAndObject(ProcessState state) {
        List<Object> objects = new ArrayList<>();
        Long accountId = null;
        if (state.getResource() instanceof Nic) {
            Nic nic = (Nic) state.getResource();
            Instance instance = objectManager.loadResource(Instance.class, nic.getInstanceId());
            addInstance(objects, instance);
            accountId = instance.getAccountId();
        } else if (state.getResource() instanceof Instance) {
            Instance instance = (Instance) state.getResource();
            addInstance(objects, instance);
            accountId = instance.getAccountId();
        } else if (state.getResource() instanceof ServiceConsumeMap) {
            ServiceConsumeMap consumeMap = (ServiceConsumeMap) state.getResource();
            Service svc = objectManager.loadResource(Service.class, consumeMap.getServiceId());
            objects.add(svc);
            accountId = svc.getAccountId();
        } else if (state.getResource() instanceof ServiceExposeMap) {
            ServiceExposeMap exposeMap = (ServiceExposeMap) state.getResource();
            Service svc = objectManager.loadResource(Service.class, exposeMap.getServiceId());
            objects.add(svc);
            accountId = svc.getAccountId();
        } else if (state.getResource() instanceof Service) {
            Service svc = (Service) state.getResource();
            objects.add(svc);
            objects.addAll(objectManager.find(Environment.class, ENVIRONMENT.ID, svc.getEnvironmentId(),
                    ENVIRONMENT.REMOVED, null));
            accountId = svc.getAccountId();
        } else if (state.getResource() instanceof Host) {
            Host host = (Host) state.getResource();
            objects.add(host);
            accountId = host.getAccountId();
        } else if (state.getResource() instanceof Account) {
            Account account = (Account)state.getResource();
            //update all the stacks
            objects.addAll(objectManager.find(Environment.class, ENVIRONMENT.ACCOUNT_ID, account.getId(),
                    ENVIRONMENT.REMOVED, null));
            accountId = account.getId();
        }
        return Pair.of(accountId, objects);
    }

    protected void addInstance(List<Object> objects, Instance instance) {
        objects.add(instance);
        objects.addAll(instanceDao.findServicesFor(instance));
    }

}
