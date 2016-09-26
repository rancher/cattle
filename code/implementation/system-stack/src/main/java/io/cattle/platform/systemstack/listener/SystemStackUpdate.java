package io.cattle.platform.systemstack.listener;

import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.tables.records.StackRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
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
import io.cattle.platform.systemstack.listener.external.api.catalog.Template;
import io.cattle.platform.systemstack.lock.SystemStackLock;
import io.cattle.platform.systemstack.process.SystemStackTrigger;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import com.netflix.config.DynamicStringProperty;

public class SystemStackUpdate extends AbstractJooqDao implements AnnotatedEventListener {

    public static final String KUBERNETES_STACK = "kubernetes";
    public static final String SWARM_STACK = "swarm";
    public static final String MESOS_STACK = "mesos";
    public static final String PUBLIC_DNS_STACK = "publicDns";
    public static final String VIRTUAL_MACHINE_STACK = "virtualMachine";

    public static final Map<String, String> ALTERNATIVE_NAME = new HashMap<>();
    static {
        ALTERNATIVE_NAME.put(KUBERNETES_STACK, "k8s");
    }

    public static final String[] STACKS = new String[] { SWARM_STACK, KUBERNETES_STACK, MESOS_STACK, PUBLIC_DNS_STACK,
            VIRTUAL_MACHINE_STACK };
    public static final Map<String, String> STACK_EXTERNAL_IDS = new HashMap<>();
    static {
        STACK_EXTERNAL_IDS.put("swarm", "system://%s%s");
        STACK_EXTERNAL_IDS.put("mesos", "system://%s%s");
        STACK_EXTERNAL_IDS.put("virtualMachine", "system://%s%s");
        STACK_EXTERNAL_IDS.put("publicDns", "system-catalog://library:%s:%s");
        STACK_EXTERNAL_IDS.put("kubernetes", "system-catalog://library:%s:%s");
    }
    private static final String STACK_RESOURCE = "/config-content/system-stacks/%s/%s";
    private static final Logger log = LoggerFactory.getLogger(SystemStackUpdate.class);
    private static DynamicStringProperty CATALOG_RESOURCE_URL = ArchaiusUtil.getString("system.stack.catalog.url");
    private static DynamicStringProperty CATALOG_RESOURCE_VERSION = ArchaiusUtil.getString("rancher.server.version");

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
        Stack env = getStack(account, stack);
        if (env != null && CommonStatesConstants.REMOVING.equals(env.getState())) {
            /* Stack is deleting, so just wait */
            return false;
        }

        if (env != null) {
            return true;
        }

        String compose, rancherCompose;

        compose = getFile(stack, "docker-compose.yml");

        String version = "";
        if (compose == null) {
            //not found in classpath, fetch from catalog
            Template catalogEntry = getTemplateFromCatalog(stack);
            if(catalogEntry == null) {
                log.error("Failed to load template from catalog for stack [{}]", stack);
                return true;
            }
            compose = catalogEntry.getDockerCompose();
            rancherCompose = catalogEntry.getRancherCompose();
            version = catalogEntry.getId().substring(catalogEntry.getId().lastIndexOf(':') + 1);
        } else {
            rancherCompose = getFile(stack, "rancher-compose.yml");
        }

        if (compose == null) {
            log.error("Failed to find compose file for stack [{}]", stack);
            return true;
        }

        Map<Object, Object> data = CollectionUtils.asMap(
                (Object)STACK.NAME, StringUtils.capitalize(stack),
                STACK.ACCOUNT_ID, account.getId(),
                STACK.EXTERNAL_ID, getExternalId(stack, version, true),
                ServiceConstants.STACK_FIELD_DOCKER_COMPOSE, compose,
                ServiceConstants.STACK_FIELD_RANCHER_COMPOSE, rancherCompose,
                ServiceConstants.STACK_FIELD_START_ON_CREATE, true);

        Map<String, Object> props = objectManager.convertToPropertiesFor(Stack.class, data);
        props.put("isSystem", true);
        resourceDao.createAndSchedule(Stack.class, props);
        return true;
    }

    public static String getExternalId(String stackType, String version, boolean alternativeName) {
        if (alternativeName && ALTERNATIVE_NAME.containsKey(stackType)) {
            return String.format(STACK_EXTERNAL_IDS.get(stackType), ALTERNATIVE_NAME.get(stackType), version);
        }
        return String.format(STACK_EXTERNAL_IDS.get(stackType), stackType, version);
    }

    public static String getStackTypeFromExternalId(String externalId) {
        for (String stackType : STACK_EXTERNAL_IDS.keySet()) {
            if (ALTERNATIVE_NAME.containsKey(stackType)) {
                if (externalId.startsWith(getExternalId(stackType, "", true))) {
                    return stackType;
                }
            }
            if (externalId.startsWith(getExternalId(stackType, "", false))) {
                return stackType;
            }
        }
        return null;
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
        if (ALTERNATIVE_NAME.containsKey(stack)) {
            stack = ALTERNATIVE_NAME.get(stack);
        }
        StringBuilder catalogTemplateUrl = new StringBuilder(CATALOG_RESOURCE_URL.get());
        catalogTemplateUrl.append(":").append(stack);
        String minVersion = CATALOG_RESOURCE_VERSION.get();
        if (!StringUtils.isEmpty(minVersion)) {
            catalogTemplateUrl.append("?minimumRancherVersion_lte=")
                    .append(minVersion);
        }

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

        if (template != null && template.getVersionLinks() != null) {
            String versionUrl = null;
            String defaultVersionURL = template.getVersionLinks().get(template.getDefaultVersion());
            if (!StringUtils.isEmpty(defaultVersionURL)) {
                versionUrl = defaultVersionURL;
            } else {
                long maxVersion = 0;
                for (String url : template.getVersionLinks().values()) {
                    long currentMaxVersion = Long.valueOf(url.substring(url.lastIndexOf(":") + 1, url.length()));
                    if (currentMaxVersion >= maxVersion) {
                        maxVersion = Long.valueOf(currentMaxVersion);
                        versionUrl = url;
                    }
                }
            }
            if (versionUrl == null) {
                return null;
            }
            log.debug("Catalog system template versionUrl is: [{}]", versionUrl);

            Request versionReq = Request.Get(versionUrl);
            Response versionRes = versionReq.execute();
            Template templateVersion = jsonMapper.readValue(versionRes.returnContent().asBytes(), Template.class);

            return templateVersion;
        }

        return null;

    }

    protected Stack getStack(Account account, String stackType) {
        List<Stack> stacks = new ArrayList<>();
        List<String> externalIds = new ArrayList<>();
        externalIds.add(getExternalId(stackType, "", false));
        externalIds.add(getExternalId(stackType, "", true));
        for (String externalId : externalIds) {
            stacks.addAll(create()
                .select(STACK.fields())
                .from(STACK)
                .where(STACK.REMOVED.isNull())
                .and(STACK.ACCOUNT_ID.eq(account.getId()))
                    .and(STACK.EXTERNAL_ID.startsWith(externalId))
                    .fetchInto(StackRecord.class));
        }

        if (stacks.isEmpty()) {
            return null;
        }
        return stacks.get(0);
    }

    protected void removeStack(Account account, String stackType) {
        Stack env = getStack(account, stackType);
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
