package io.github.ibuildthecloud.dstack.filter.agent;

import io.github.ibuildthecloud.dstack.db.jooq.generated.model.Agent;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;

public class AgentFilter extends AbstractResourceManagerFilter {

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Agent.class };
    }

}
