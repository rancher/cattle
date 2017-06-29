package io.cattle.platform.servicediscovery.api.action;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

public class StackActivateServicesActionHandler implements ActionHandler {

    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;
    ServiceConsumeMapDao consumeMapDao;

    public StackActivateServicesActionHandler(ObjectProcessManager objectProcessManager, ObjectManager objectManager, ServiceConsumeMapDao consumeMapDao) {
        super();
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
        this.consumeMapDao = consumeMapDao;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }
        Stack env = (Stack) obj;
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.STACK_ID, env.getId(),
                SERVICE.REMOVED,
                null);
        activateServices(services);

        return env;
    }

    private void activateServices(List<? extends Service> services) {
        List<Long> alreadyActivatedServices = new ArrayList<>();
        List<Long> alreadySeenServices = new ArrayList<>();
        Map<Long, Service> servicesToActivate = new HashMap<>();
        for (Service service : services) {
            servicesToActivate.put(service.getId(), service);
        }

        for (Service service : services) {

            activateService(service, servicesToActivate, alreadySeenServices, alreadyActivatedServices);
        }
    }

    @SuppressWarnings("unchecked")
    protected void activateService(Service service, Map<Long, Service> servicesToActivate,
            List<Long> alreadySeenServices, List<Long> alreadyActivatedServices) {
        if (alreadyActivatedServices.contains(service.getId())) {
            return;
        }
        alreadySeenServices.add(service.getId());
        List<Long> consumedServicesIds = (List<Long>) CollectionUtils.collect(
                consumeMapDao.findConsumedServices(service.getId()),
                TransformerUtils.invokerTransformer("getConsumedServiceId"));
        for (Long consumedServiceId : consumedServicesIds) {
            Service consumedService = servicesToActivate.get(consumedServiceId);
            if (consumedService != null && !alreadySeenServices.contains(consumedService.getId())) {
                activateService(consumedService, servicesToActivate, alreadySeenServices, alreadyActivatedServices);
            }
        }

        if (service.getState().equalsIgnoreCase(CommonStatesConstants.INACTIVE)) {
            Map<String, Object> data = new HashMap<>();
            List<Long> consumedServicesToWaitFor = new ArrayList<>();
            consumedServicesToWaitFor.addAll(consumedServicesIds);
            consumedServicesToWaitFor.retainAll(alreadyActivatedServices);
            data.put(ServiceConstants.FIELD_WAIT_FOR_CONSUMED_SERVICES_IDS, consumedServicesToWaitFor);
            objectProcessManager.scheduleStandardProcess(StandardProcess.ACTIVATE, service, data);
        }
        alreadyActivatedServices.add(service.getId());
    }
}
