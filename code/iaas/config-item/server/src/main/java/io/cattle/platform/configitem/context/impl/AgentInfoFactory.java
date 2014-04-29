package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.context.dao.NetworkInfoDao;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.util.type.InitializationTask;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

@Named
public class AgentInfoFactory extends AbstractAgentBaseContextFactory implements InitializationTask {

    public static final DynamicStringListProperty ITEMS = ArchaiusUtil.getList("item.context.network.info.items");
    public static final DynamicStringProperty EXPR = ArchaiusUtil.getString("event.data.instance");

    NetworkInfoDao networkInfo;
    ObjectSerializerFactory objectSerializerFactory;
    ObjectSerializer serializer;

    @Override
    public String[] getItems() {
        return new String[] { "agent-instance-startup" };
    }

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        context.getData().put("instance", serializer.serialize(instance).values().iterator().next());
        context.getData().put("agent", agent);
    }

    public NetworkInfoDao getNetworkInfo() {
        return networkInfo;
    }

    @Inject
    public void setNetworkInfo(NetworkInfoDao networkInfo) {
        this.networkInfo = networkInfo;
    }

    public ObjectSerializerFactory getObjectSerializerFactory() {
        return objectSerializerFactory;
    }

    @Inject
    public void setObjectSerializerFactory(ObjectSerializerFactory objectSerializerFactory) {
        this.objectSerializerFactory = objectSerializerFactory;
    }

    @Override
    public void start() {
        serializer = objectSerializerFactory.compile(InstanceConstants.TYPE, EXPR.get());
    }

    @Override
    public void stop() {
    }

}
