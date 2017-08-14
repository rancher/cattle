package io.cattle.platform.agent.server.ping;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.server.resource.impl.AgentResourcesMonitor;
import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.containersync.PingInstancesMonitor;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.util.ProcessEngineUtils;
import io.cattle.platform.engine.server.Cluster;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.task.Task;
import io.cattle.platform.task.TaskOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Futures.*;

public class PingMonitor implements Task, TaskOptions {

    private static final DynamicLongProperty BAD_PINGS = ArchaiusUtil.getLong("agent.ping.reconnect.after.failed.count");
    private static final DynamicLongProperty PING_TIMEOUT = ArchaiusUtil.getLong("agent.ping.timeout.seconds");
    private static final DynamicLongProperty PING_STATS_EVERY = ArchaiusUtil.getLong("agent.ping.stats.every");
    private static final DynamicLongProperty PING_RESOURCES_EVERY = ArchaiusUtil.getLong("agent.ping.resources.every");
    private static final DynamicLongProperty PING_INSTANCES_EVERY = ArchaiusUtil.getLong("agent.ping.instances.every");
    private static final DynamicLongProperty PING_SCHEDULE = ArchaiusUtil.getLong("task.agent.ping.schedule");

    private static final Logger log = LoggerFactory.getLogger(PingMonitor.class);

    AgentResourcesMonitor agentResourceManager;
    PingInstancesMonitor pingInstanceMonitor;
    ObjectProcessManager processManager;
    ObjectManager objectManager;
    int interation = 0;
    AgentDao agentDao;
    AgentLocator agentLocator;
    Cluster cluster;
    LoadingCache<Long, PingStatus> status = CacheBuilder.newBuilder().expireAfterAccess(PING_SCHEDULE.get() * 3, TimeUnit.SECONDS).build(
            new CacheLoader<Long, PingStatus>() {
                @Override
                public PingStatus load(Long key) throws Exception {
                    return new PingStatus(key);
                }
            });


    public PingMonitor(AgentResourcesMonitor agentResourceManager, PingInstancesMonitor pingInstanceMonitor, ObjectProcessManager processManager,
                       ObjectManager objectManager, AgentDao agentDao, AgentLocator agentLocator, Cluster cluster) {
        super();
        this.agentResourceManager = agentResourceManager;
        this.pingInstanceMonitor = pingInstanceMonitor;
        this.processManager = processManager;
        this.objectManager = objectManager;
        this.agentDao = agentDao;
        this.agentLocator = agentLocator;
        this.cluster = cluster;
    }

    private void handleOwned(Agent agent) {
        Ping ping = AgentUtils.newPing(agent);

        if (isInterval(PING_STATS_EVERY.get())) {
            ping.setOption(Ping.STATS, true);
        }

        if (isInterval(PING_RESOURCES_EVERY.get())) {
            ping.setOption(Ping.RESOURCES, true);
        }

        if (isInterval(PING_INSTANCES_EVERY.get())) {
            ping.setOption(Ping.INSTANCES, true);
        }

        doPing(agent, ping);
    }

    private boolean isInterval(long every) {
        return interation % every == 0;
    }

    private void ping(Agent agent) {
        if (!cluster.isInPartition(agent.getAccountId(), agent.getClusterId())) {
            return;
        }

        handleOwned(agent);
    }

    private void doPing(Agent agent, Ping ping) {
        RemoteAgent remoteAgent = agentLocator.lookupAgent(agent);

        EventCallOptions options = new EventCallOptions(0, PING_TIMEOUT.get() * 1000);
        addCallback(remoteAgent.call(ping, Ping.class, options), new FutureCallback<Ping>() {
            @Override
            public void onSuccess(Ping pong) {
                pingSuccess(agent, pong);
            }

            @Override
            public void onFailure(Throwable t) {
                pingFailure(agent.getId());
            }
        });
    }

    public void pingSuccess(Agent agent, Ping pong) {
        status.getUnchecked(agent.getId()).success();
        agentResourceManager.processPingReply(agent, pong);
        pingInstanceMonitor.processPingReply(agent, pong);
    }

    private void pingFailure(Long agentId) {
        long count = status.getUnchecked(agentId).failed();
        if (count < 3) {
            log.info("Missed ping from agent [{}] count [{}]", agentId, count);
        } else {
            log.error("Failed to get ping from agent [{}] count [{}]", agentId, count);
        }
        if (count >= BAD_PINGS.get()) {
            try {
                Agent agent = objectManager.loadResource(Agent.class, agentId);
                if (CommonStatesConstants.ACTIVE.equals(agent.getState())) {
                    log.error("Scheduling reconnect for [{}]", agent.getId());
                    processManager.scheduleProcessInstance(AgentConstants.PROCESS_RECONNECT, agent, null);
                }
            } catch (ProcessInstanceException e) {
                if (e.getExitReason() != ExitReason.CANCELED) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void run() {
        if (!ProcessEngineUtils.enabled()) {
            return;
        }

        for (Agent agent : agentDao.findAgentsToPing()) {
            ping(agent);
        }
        interation++;
    }

    @Override
    public boolean isShouldRecord() {
        return false;
    }

    @Override
    public boolean isShouldLock() {
        return false;
    }

    @Override
    public String getName() {
        return "agent.ping";
    }

}
