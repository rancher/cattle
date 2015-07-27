package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceStartRegisterAsNspProvider extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {
    @Inject
    NetworkDao ntwkDao;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_START };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        List<String> services = getProvidedServices(instance);
        if (services.isEmpty()) {
            return null;
        }
        ntwkDao.registerNspInstance(NetworkServiceProviderConstants.KIND_EXTERNAL_PROVIDER, instance, services);
        return null;
    }

    @SuppressWarnings("unchecked")
    protected List<String> getProvidedServices(Instance instance) {
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);
        String label = labels.get(NetworkServiceConstants.LABEL_PROVIDED_SERVICES);

        List<String> services = new ArrayList<>();
        if (label != null) {
            services.addAll(Arrays.asList(label.split(",")));
        }
        return services;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
