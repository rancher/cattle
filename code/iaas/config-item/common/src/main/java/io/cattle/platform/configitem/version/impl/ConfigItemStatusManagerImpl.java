package io.cattle.platform.configitem.version.impl;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.exception.ConfigTimeoutException;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.configitem.version.dao.ConfigItemStatusDao;
import io.cattle.platform.core.model.ConfigItemStatus;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.exception.AgentRemovedException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringListProperty;

public class ConfigItemStatusManagerImpl implements ConfigItemStatusManager {

    private static final DynamicBooleanProperty BLOCK = ArchaiusUtil.getBoolean("item.migration.block.on.failure");
    private static final DynamicStringListProperty PRIORITY_ITEMS = ArchaiusUtil.getList("item.priority");

    private static final Logger log = LoggerFactory.getLogger(ConfigItemStatusManagerImpl.class);

    @Inject
    ConfigItemStatusDao configItemStatusDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    AgentLocator agentLocator;

    @Inject
    ConfigUpdatePublisher publisher;

    @Inject
    LockManager lockManager;

    @Inject
    EventService eventService;

    @Override
    public boolean runUpdateForEvent(final String itemName, final ConfigUpdate update, final Client client, final Runnable run) {
        boolean found = false;
        for (ConfigUpdateItem item : update.getData().getItems()) {
            if (itemName.equals(item.getName())) {
                found = true;
            }
        }

        if (!found) {
            return false;
        }

        return lockManager.tryLock(new ConfigItemProcessLock(itemName, client), new LockCallback<Object>() {
            @Override
            public Object doWithLock() {
                ItemVersion itemVersion = getRequestedVersion(client, itemName);
                if (itemVersion == null) {
                    return null;
                }
                run.run();
                setApplied(client, itemName, itemVersion);
                eventService.publish(EventVO.reply(update));
                return new Object();
            }
        }) != null;
    }


    protected Map<String, ConfigItemStatus> getStatus(ConfigUpdateRequest request) {
        Map<String, ConfigItemStatus> statuses = new HashMap<String, ConfigItemStatus>();

        for (ConfigItemStatus status : configItemStatusDao.listItems(request)) {
            statuses.put(status.getName(), status);
        }

        return statuses;
    }

    @Override
    public void updateConfig(ConfigUpdateRequest request) {
        if (request.getClient() == null) {
            throw new IllegalArgumentException("Client is null on request [" + request + "]");
        }

        log.trace("ITEM UPDATE: for [{}]", request.getClient());
        Client client = request.getClient();
        Map<String, ConfigItemStatus> statuses = getStatus(request);
        List<ConfigUpdateItem> toTrigger = new ArrayList<ConfigUpdateItem>();

        for (ConfigUpdateItem item : request.getItems()) {
            String name = item.getName();
            ConfigItemStatus status = statuses.get(name);
            Long requestedVersion = item.getRequestedVersion();

            if (status == null) {
                if (item.isApply()) {
                    log.trace("ITEM UPDATE: incrementOrApply [{}]", request.getClient());
                    configItemStatusDao.incrementOrApply(client, name);
                    log.trace("ITEM UPDATE: done incrementOrApply [{}]", request.getClient());
                } else {
                    log.info("ITEM UPDATE: ignore [{}] [{}]", name, request.getClient());
                    continue;
                }
                log.trace("ITEM UPDATE: get requested [{}]", request.getClient());
                requestedVersion = configItemStatusDao.getRequestedVersion(client, name);
                log.trace("ITEM UPDATE: done get requested [{}]", request.getClient());
            } else if (requestedVersion == null && item.getSetVersion() != null) {
                log.trace("ITEM UPDATE: setVersion [{}]", request.getClient());
                configItemStatusDao.setIfOlder(client, name, item.getSetVersion());
                log.trace("ITEM UPDATE: done setVersion [{}]", request.getClient());
                requestedVersion = item.getSetVersion();
            } else if (requestedVersion == null && item.isIncrement()) {
                log.trace("ITEM UPDATE: incrementOrApply [{}]", request.getClient());
                configItemStatusDao.incrementOrApply(client, name);
                log.trace("ITEM UPDATE: done incrementOrApply [{}]", request.getClient());
                requestedVersion = status.getRequestedVersion() + 1;
            } else if (requestedVersion == null) {
                requestedVersion = status.getRequestedVersion();
            }

            item.setRequestedVersion(requestedVersion);
            toTrigger.add(item);
        }

        triggerUpdate(request, toTrigger);
    }

    protected void triggerUpdate(final ConfigUpdateRequest request, final List<ConfigUpdateItem> items) {
        final ConfigUpdate event = getEvent(request, items);
        if (event == null) {
            return;
        }

        Runnable run = new Runnable() {
            @Override
            public void run() {
                request.setUpdateFuture(call(request.getClient(), event));
            }
        };

        if (request.isDeferredTrigger()) {
            DeferredUtils.defer(run);
        } else {
            run.run();
        }
    }

    private ListenableFuture<? extends Event> call(Client client, ConfigUpdate event) {
        return publisher.publish(client, event);
    }

    protected ConfigUpdate getEvent(ConfigUpdateRequest request, List<ConfigUpdateItem> items) {
        Client client = request.getClient();
        String url = ServerContext.getHostApiBaseUrl(BaseProtocol.HTTP);

        if (items.size() == 0) {
            return new ConfigUpdate(client.getEventName(), url, Collections.<ConfigUpdateItem> emptyList());
        }

        ConfigUpdate event = new ConfigUpdate(client.getEventName(), url, items);

        event.withResourceType(objectManager.getType(client.getResourceType())).withResourceId(Long.toString(client.getResourceId()));

        return event;
    }

    protected ConfigUpdate getEvent(ConfigUpdateRequest request) {
        List<ConfigUpdateItem> toTrigger = getNeedsUpdating(request, !request.isMigration());
        return getEvent(request, toTrigger);
    }

    @Override
    public ListenableFuture<?> whenReady(final ConfigUpdateRequest request) {
        ConfigUpdate event = getEvent(request);

        if (event.getData().getItems().size() == 0) {
            return AsyncUtils.done();
        }

        ListenableFuture<? extends Event> future = request.getUpdateFuture();
        if (future == null) {
            future = call(request.getClient(), event);
        }

        return Futures.transform(future, new Function<Event, Object>() {
            @Override
            public Object apply(Event input) {
                logResponse(request, input);
                List<ConfigUpdateItem> toTrigger = getNeedsUpdating(request, true);
                if (toTrigger.size() > 0) {
                    throw new ConfigTimeoutException(request, toTrigger);
                }

                return Boolean.TRUE;
            }
        });
    }

    protected List<ConfigUpdateItem> getNeedsUpdating(ConfigUpdateRequest request, boolean checkVersions) {
        Client client = request.getClient();
        Map<String, ConfigItemStatus> statuses = getStatus(request);
        List<ConfigUpdateItem> toTrigger = new ArrayList<ConfigUpdateItem>();

        for (ConfigUpdateItem item : request.getItems()) {
            String name = item.getName();
            ConfigItemStatus status = statuses.get(item.getName());

            if (status == null) {
                log.error("Waiting on config item [{}] on client [{}] but it is not applied", name, client);
                continue;
            }

            if (item.isCheckInSyncOnly()) {
                if (!checkVersions || !ObjectUtils.equals(status.getRequestedVersion(), status.getAppliedVersion())) {
                    if (request.isMigration()) {
                        log.info("Waiting on [{}] on [{}], for migration", client, name);
                    } else {
                        log.debug("Waiting on [{}] on [{}], not in sync requested [{}] != applied [{}]", client, name, status.getRequestedVersion(), status
                                .getAppliedVersion());
                    }
                    addToList(toTrigger, item);
                }
            } else if (item.getRequestedVersion() != null) {
                Long applied = status.getAppliedVersion();
                if (applied == null || item.getRequestedVersion() > applied) {
                    log.debug("Waiting on [{}] on [{}], not applied requested [{}] > applied [{}]", client, name, item.getRequestedVersion(), applied);
                    addToList(toTrigger, item);
                }
            }
        }

        return toTrigger;
    }

    protected static void addToList(List<ConfigUpdateItem> list, ConfigUpdateItem item) {
        if (PRIORITY_ITEMS.get().contains(item.getName())) {
            list.add(0, item);
        } else {
            list.add(item);
        }
    }

    @Override
    public void waitFor(ConfigUpdateRequest request) {
        AsyncUtils.get(whenReady(request));
    }


    @Override
    public void sync(final boolean migration) {
        Map<Client, List<String>> items = configItemStatusDao.findOutOfSync(migration);

        boolean first = true;
        for (final Map.Entry<Client, List<String>> entry : items.entrySet()) {
            final Client client = entry.getKey();
            final ConfigUpdateRequest request = new ConfigUpdateRequest(client).withMigration(migration);

            for (String item : entry.getValue()) {
                request.addItem(item).withApply(false).withIncrement(false).withCheckInSyncOnly(true);
            }

            log.info("Requesting {} of item(s) {} on [{}]", migration ? "migration" : "update", entry.getValue(), client);

            if (first && migration && BLOCK.get()) {
                waitFor(request);
            } else {
                ConfigUpdate event = getEvent(request);
                ListenableFuture<? extends Event> future = call(client, event);
                Futures.addCallback(future, new FutureCallback<Event>() {
                    @Override
                    public void onSuccess(Event result) {
                        logResponse(request, result);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if (t instanceof TimeoutException) {
                            log.info("Timeout {} item(s) {} on [{}]", migration ? "migrating" : "updating", entry.getValue(), client);
                        } else if (t instanceof AgentRemovedException) {
                            log.info("Agent removed {} item(s) {} on [{}]", migration ? "migrating" : "updating", entry.getValue(), client);
                        } else {
                            log.error("Error {} item(s) {} on [{}]", migration ? "migrating" : "updating", entry.getValue(), client, t);
                        }
                    }
                });
            }

            first = false;
        }
    }

    protected static void logResponse(ConfigUpdateRequest request, Event event) {
        Map<String, Object> data = CollectionUtils.toMap(event.getData());

        Object exitCode = data.get("exitCode");
        Object output = data.get("output");

        if (exitCode != null) {
            long exit = Long.parseLong(exitCode.toString());

            if (exit == 0) {
                log.debug("Success {}", request);
            } else if (exit == 122 && "Lock failed".equals(output)) {
                /*
                 * This happens when the lock fails to apply. Really we should
                 * upgrade to newer util-linux that supports -E and then set a
                 * special exit code. That will be slightly better
                 */
                log.info("Failed {}, exit code [{}] output [{}]", request, exitCode, output);
            } else {
                log.error("Failed {}, exit code [{}] output [{}]", request, exitCode, output);
            }
        }
    }

    @Override
    public boolean setApplied(Client client, String itemName, ItemVersion version) {
        return configItemStatusDao.setApplied(client, itemName, version);
    }

    @Override
    public void setLatest(Client client, String itemName, String sourceRevision) {
        configItemStatusDao.setLatest(client, itemName, sourceRevision);
    }

    @Override
    public boolean isAssigned(Client client, String itemName) {
        return configItemStatusDao.isAssigned(client, itemName);
    }

    @Override
    public void setItemSourceVersion(String name, String sourceRevision) {
        configItemStatusDao.setItemSourceVersion(name, sourceRevision);
    }

    @Override
    public ItemVersion getRequestedVersion(Client client, String itemName) {
        return configItemStatusDao.getRequestedItemVersion(client, itemName);
    }
}
