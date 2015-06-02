package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentUnitInstanceIdGeneratorImpl implements DeploymentUnitInstanceIdGenerator {
    Map<String, List<Integer>> launchConfigUsedIds = new HashMap<>();
    
    public DeploymentUnitInstanceIdGeneratorImpl(Map<String, List<Integer>> launchConfigUsedIds) {
        this.launchConfigUsedIds = launchConfigUsedIds;
    }
    
    @Override
    public synchronized Integer getNextAvailableId(String launchConfigName) {
        List<Integer> usedIds = launchConfigUsedIds.get(launchConfigName);
        Collections.sort(usedIds);
        Integer newId = getNewId(launchConfigName);
        usedIds.add(newId);
        Collections.sort(usedIds);
        launchConfigUsedIds.put(launchConfigName, usedIds);
        return newId;
    }

    protected Integer getNewId(String launchConfigName) {
        List<Integer> usedIds = launchConfigUsedIds.get(launchConfigName);
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
