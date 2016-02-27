package io.cattle.platform.systemstack.listener;

import static io.cattle.platform.core.model.tables.EnvironmentTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.lock.EventLock;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.systemstack.listener.external.api.catalog.Template;
import io.cattle.platform.systemstack.lock.SystemStackLock;
import io.cattle.platform.systemstack.process.SystemStackTrigger;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemStackUpdate implements AnnotatedEventListener {

    private static final String[] STACKS = new String[] {"swarm", "kubernetes", "publicDns"};
    private static final String STACK_EXTERNAL_ID = "system://%s";
    private static final String STACK_RESOURCE = "/config-content/system-stacks/%s/%s";
    private static final Logger log = LoggerFactory.getLogger(SystemStackUpdate.class);
    private static final String CATALOG_RESOURCE_URL = ArchaiusUtil.getString("system.stack.catalog.url").get();

    @Inject
    ObjectProcessManager processManager;

    @Inject
    ConfigItemStatusManager itemManager;

    @Inject
    EventService eventService;

    @Inject
    ObjectManager objectManager;

    @Inject
    HostDao hostDao;

    @Inject
    LockManager lockManager;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    JsonMapper jsonMapper;

    @EventHandler(lock=EventLock.class)
    public void globalServiceUpdate(ConfigUpdate update) {
        if (update.getResourceId() == null) {
            return;
        }

        final Client client = new Client(Account.class, new Long(update.getResourceId()));
        reconcileForClient(update, client, new Callable<Boolean>() {
            @Override
            public Boolean call() throws IOException {
                return process(client.getResourceId());
            }
        });
    }

    protected boolean process(long accountId) throws IOException {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null || account.getRemoved() != null) {
            return true;
        }

        if (hostDao.getActiveHosts(account.getId()).size() <= 0) {
            return true;
        }

        for (String stack : STACKS) {
            boolean enabled = DataAccessor.fieldBool(account, stack);
            if (enabled) {
                if (!addStack(account, stack)) {
                    return false;
                }
            } else {
                removeStack(account, stack);
            }
        }

        return true;
    }

    protected boolean addStack(Account account, String stack) throws IOException {
        Environment env = getStack(account, stack);
        if (env != null && CommonStatesConstants.REMOVING.equals(env.getState())) {
            /* Stack is deleting, so just wait */
            return false;
        }

        if (env != null) {
            return true;
        }

        String compose, rancherCompose;

        compose = getFile(stack, "docker-compose.yml");

        if (compose == null) {
            //not found in classpath, fetch from catalog
            Template catalogEntry = getTemplateFromCatalog(stack);
            if(catalogEntry == null) {
                log.error("Failed to load template from catalog for stack [{}]", stack);
                return true;
            }
            compose = catalogEntry.getDockerCompose();
            rancherCompose = catalogEntry.getRancherCompose();
        } else {
            rancherCompose = getFile(stack, "rancher-compose.yml");
        }

        if (compose == null) {
            log.error("Failed to find compose file for stack [{}]", stack);
            return true;
        }

        Map<Object, Object> data = CollectionUtils.asMap(
                (Object)ENVIRONMENT.NAME, StringUtils.capitalize(stack),
                ENVIRONMENT.ACCOUNT_ID, account.getId(),
                ENVIRONMENT.EXTERNAL_ID, String.format(STACK_EXTERNAL_ID, stack),
                ServiceDiscoveryConstants.STACK_FIELD_DOCKER_COMPOSE, compose,
                ServiceDiscoveryConstants.STACK_FIELD_RANCHER_COMPOSE, rancherCompose,
                ServiceDiscoveryConstants.STACK_FIELD_START_ON_CREATE, true);

        Map<String, Object> props = objectManager.convertToPropertiesFor(Environment.class, data);
        resourceDao.createAndSchedule(Environment.class, props);
        return true;
    }

    protected String getFile(String stack, String filename) throws IOException {
        String resource = String.format(STACK_RESOURCE, stack, filename);
        InputStream is = getClass().getResourceAsStream(resource);
        if (is == null) {
            return null;
        }

        return IOUtils.toString(is);
    }

    protected Template getTemplateFromCatalog(String stack) throws IOException {

        StringBuilder catalogTemplateUrl = new StringBuilder(CATALOG_RESOURCE_URL);
        catalogTemplateUrl.append(":").append(stack);
        //get the latest version from the catalog template
        Request temp = Request.Get(catalogTemplateUrl.toString());
        Response res;
        try {
           res = temp.execute();
        } catch(HttpHostConnectException ex) {
            log.error("Catalog Service not reachable at [{}]", CATALOG_RESOURCE_URL);
            return null;
        }
        Template template = jsonMapper.readValue(res.returnContent().asBytes(), Template.class);

        if (template != null && template.getDefaultVersion() != null && template.getVersionLinks() != null) {
            String versionUrl = template.getVersionLinks().get(template.getDefaultVersion());
            log.debug("Catalog system template versionUrl is: [{}]", versionUrl);

            Request versionReq = Request.Get(versionUrl);
            Response versionRes = versionReq.execute();
            Template templateVersion = jsonMapper.readValue(versionRes.returnContent().asBytes(), Template.class);

            return templateVersion;
        }

        return null;

    }

    protected Environment getStack(Account account, String stack) {
        return objectManager.findAny(Environment.class,
                ENVIRONMENT.ACCOUNT_ID, account.getId(),
                ENVIRONMENT.EXTERNAL_ID, String.format(STACK_EXTERNAL_ID, stack),
                ENVIRONMENT.REMOVED, null);
    }

    protected void removeStack(Account account, String stack) {
        Environment env = getStack(account, stack);
        if (env != null) {
            processManager.scheduleStandardProcess(StandardProcess.REMOVE, env, null);
        }
    }

    protected void reconcileForClient(final ConfigUpdate update, final Client client, final Callable<Boolean> run) {
        lockManager.lock(new SystemStackLock(client.getResourceId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                ItemVersion itemVersion = itemManager.getRequestedVersion(client, SystemStackTrigger.STACKS);
                if (itemVersion == null) {
                    return;
                }
                try {
                    if (run.call()) {
                        itemManager.setApplied(client, SystemStackTrigger.STACKS, itemVersion);
                        eventService.publish(EventVO.reply(update));
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to process system stacks for [" + client.getResourceId() + "]", e);
                }
            }
        });
    }
}
