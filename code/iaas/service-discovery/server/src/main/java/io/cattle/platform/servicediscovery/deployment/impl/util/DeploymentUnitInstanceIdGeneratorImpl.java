package io.cattle.platform.servicediscovery.deployment.impl.util;

import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeploymentUnitInstanceIdGeneratorImpl implements DeploymentUnitInstanceIdGenerator {
    List<Integer> usedIds = new ArrayList<>();

    public DeploymentUnitInstanceIdGeneratorImpl(List<Integer> usedIds) {
        this.usedIds = usedIds;
    }
    
    @Override
    public synchronized Integer getNextAvailableId(String launchConfigName) {
        Collections.sort(usedIds);
        Integer newId = getNewId(launchConfigName);
        usedIds.add(newId);
        Collections.sort(usedIds);
        return newId;
    }

    protected Integer getNewId(String launchConfigName) {
        return generateNewId(usedIds);
    }

    @Override
    public synchronized Integer getNextAvailableId() {
        Collections.sort(usedIds);
        Integer newId = generateNewId(usedIds);
        usedIds.add(newId);
        Collections.sort(usedIds);
        return newId;
    }

    public Integer generateNewId(List<Integer> usedIds) {
        Collections.sort(usedIds);
        Integer idToReturn = null;
        if (usedIds.size() == 0) {
            idToReturn = 1;
        } else {
            // in situation when service with scale=3 has one container missing (it got destroyed outside of the
            // service)
            // and container names don't reflect the order, we should pick the least available number that is <=order
            for (int i = 1; i <= usedIds.size(); i++) {
                if (!usedIds.contains(i)) {
                    idToReturn = i;
                    break;
                }
            }
            // if there are no gaps, get the next available
            if (idToReturn == null) {
                idToReturn = usedIds.get(usedIds.size() - 1) + 1;
            }
        }

        return idToReturn;
    }
}
