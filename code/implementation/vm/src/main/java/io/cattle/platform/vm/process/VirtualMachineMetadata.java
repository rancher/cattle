package io.cattle.platform.vm.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.metadata.service.MetadataService;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualMachineMetadata extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    private static Logger log = LoggerFactory.getLogger(VirtualMachineMetadata.class);

    @Inject
    MetadataService metadataService;

    @Inject
    IdFormatter idFormatter;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic) state.getResource();
        Instance instance = objectManager.loadResource(Instance.class, nic.getInstanceId());
        if (instance == null) {
            return null;
        }

        Map<String, Object> labels = new HashMap<>(DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS));
        if (!"true".equals(labels.get(SystemLabels.LABEL_VM))) {
            return null;
        }

        Map<String, Object> metadata = metadataService.getMetadataForInstance(instance, idFormatter);
        Map<String, Object> osMetadata = metadataService.getOsMetadata(instance, metadata);
        try {
            labels.put(SystemLabels.LABEL_VM_METADATA, jsonMapper.writeValueAsString(metadata));
        } catch (IOException e) {
            log.error("Failed to marshall metadata", e);
        }
        try {
            labels.put(SystemLabels.LABEL_VM_OS_METADATA, jsonMapper.writeValueAsString(osMetadata));
        } catch (IOException e) {
            log.error("Failed to marshall metadata", e);
        }

        objectManager.setFields(instance, InstanceConstants.FIELD_LABELS, labels);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

}
