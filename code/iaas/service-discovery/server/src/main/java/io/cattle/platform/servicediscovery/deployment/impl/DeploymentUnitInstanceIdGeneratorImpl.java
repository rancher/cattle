package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;

import java.util.Collections;
import java.util.List;

public class DeploymentUnitInstanceIdGeneratorImpl implements DeploymentUnitInstanceIdGenerator {
    Service service;
    Environment env;
    List<Integer> usedIds;
    
    public DeploymentUnitInstanceIdGeneratorImpl(Environment env, Service service, List<Integer> usedIds) {
        this.service = service;
        this.env = env;
        this.usedIds = usedIds;
    }
    
    @Override
    public synchronized Integer getNextAvailableId() {
        Collections.sort(this.usedIds);
        Integer newId = getNewId();
        usedIds.add(newId);
        Collections.sort(this.usedIds);
        return newId;
    }

    protected Integer getNewId() {
        Integer idToReturn = null;
        if (usedIds.size() == 0) {
            idToReturn = 1;
        } else {
            // in situation when service with scale=3 has one container missing (it got destroyed outside of the
            // service)
            // and container names don't reflect the order, we should pick the least available number that is <=order
            for (int i = 1; i < usedIds.size(); i++) {
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
