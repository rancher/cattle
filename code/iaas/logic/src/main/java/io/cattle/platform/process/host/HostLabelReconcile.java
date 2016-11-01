package io.cattle.platform.process.host;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostLabelMap;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostLabelReconcile extends AbstractObjectProcessHandler implements Priority {

    @Inject
    GenericMapDao mapDao;

    @Inject
    LabelsDao labelsDao;

    @Inject
    LabelsService labelsService;

    @Override
    public String[] getProcessNames() {
        return new String[] { HostConstants.PROCESS_ACTIVATE, HostConstants.PROCESS_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        @SuppressWarnings("unchecked")
        Map<String, String> labelsField = DataAccessor.fields(host).withKey(HostConstants.FIELD_LABELS).as(Map.class);

        if (labelsField == null) {
            return null;
        }

        List<Label> existing = labelsDao.getLabelsForHost(host.getId());
        List<? extends HostLabelMap> existingInstanceMappings = mapDao.findNonRemoved(HostLabelMap.class, Host.class, host.getId());

        // figure out potentially new labels to create and associate with host
        Map<String, String> existingLabels = new HashMap<String, String>();
        Map<Long, Label> existingLabelLookupById = new HashMap<Long, Label>();
        for (Label existingLabel : existing) {
            existingLabels.put(existingLabel.getKey(), existingLabel.getValue());
            existingLabelLookupById.put(existingLabel.getId(), existingLabel);
        }
        for (Map.Entry<String, String> inputEntry : labelsField.entrySet()) {
            String existingValue = existingLabels.get(inputEntry.getKey());
            if (existingValue == null || !existingValue.equals(inputEntry.getValue())) {
                labelsService.createHostLabel(
                        host.getAccountId(), host.getId(), inputEntry.getKey(), inputEntry.getValue());
            }
        }

        // figure out which mappings to remove
        for (HostLabelMap mapping : existingInstanceMappings) {
            Long labelId = mapping.getLabelId();
            Label existingLabel = existingLabelLookupById.get(labelId);
            String newLabelValue = labelsField.get(existingLabel.getKey());
            if (newLabelValue == null || !newLabelValue.equals(existingLabel.getValue())) {
                objectProcessManager.scheduleProcessInstance("hostlabelmap.remove", mapping, null);
            }
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
