package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.ContainerEvent;

import java.util.Map;

public interface ContainerEventDao {

    public boolean createContainerEvent(ContainerEvent event, Map<String, Object> data);

    boolean canCreate(Long hostId, String event);

}
