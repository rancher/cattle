package io.cattle.platform.agent.instance.service;

import io.cattle.platform.core.model.Nic;

import java.util.List;

public interface InstanceNicLookup {

    List<? extends Nic> getNics(Object obj);

}
