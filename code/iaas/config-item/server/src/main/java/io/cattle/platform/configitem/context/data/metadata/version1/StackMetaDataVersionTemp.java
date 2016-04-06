package io.cattle.platform.configitem.context.data.metadata.version1;

import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.StackMetaData;
import io.cattle.platform.core.constants.CommonStatesConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StackMetaDataVersionTemp extends StackMetaData {
    List<String> services = new ArrayList<>();

    public StackMetaDataVersionTemp(StackMetaData stackData) {
        super(stackData);
        List<String> removedStates = Arrays.asList(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING);
        for (ServiceMetaData s : super.services) {
            if (removedStates.contains(s.getState())) {
                continue;
            }
            this.services.add(s.getMetadataUuid());
        }
    }

    public List<String> getServices() {
        return this.services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
