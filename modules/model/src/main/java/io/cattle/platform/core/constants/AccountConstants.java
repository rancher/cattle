package io.cattle.platform.core.constants;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.ServicesPortRange;
import io.cattle.platform.core.util.PortRangeSpec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class AccountConstants {

    /*
     * Read from config file to support upgrade scenario when default port range is not set on account
     */
    private static final DynamicStringProperty ENV_PORT_RANGE = ArchaiusUtil
            .getString("environment.services.port.range");

    public static final String TYPE = "account";
    public static final String REGISTERED_AGENT_KIND = "registeredAgent";
    public static final String AGENT_KIND = "agent";
    public static final String SERVICE_KIND = "service";
    public static final String USER_KIND = "user";
    public static final String ADMIN_KIND = "admin";
    public static final String SUPER_ADMIN_KIND = "superadmin";
    public static final String PROJECT_KIND = "project";
    public static final String FIELD_PORT_RANGE = "servicesPortRange";
    public static final String FIELD_CREATED_STACKS = "createdStackIds";
    public static final String FIELD_STARTED_STACKS = "startedStackIds";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_HOST_REMOVE_DELAY = "hostRemoveDelaySeconds";
    public static final String FIELD_SCHEDULED_UPGRADE_DELAY = "scheduledUpgradeDelayMinutes";

    public static final DynamicStringProperty ACCOUNT_VERSION = ArchaiusUtil.getString("account.version");
    public static final String SYSTEM_UUID = "system";

    public static final String OPTION_CREATE_APIKEY = "createApiKey";
    public static final String OPTION_CREATE_APIKEY_KIND = "createApiKeyKind";

    public static final String FIELD_DEFAULT_NETWORK_ID = "defaultNetworkId";
    public static final String FIELD_ALLOW_SYSTEM_ROLE = "allowSystemRole";
    public static final String FIELD_VIRTUAL_MACHINE = "virtualMachine";
    public static final String FIELD_ORCHESTRATION = "orchestration";

    public static final String DATA_ACT_AS_RESOURCE_ACCOUNT = "actAsResourceAccount";
    public static final String DATA_AGENT_OWNER_ID = "agentOwnerId";

    public static final String DATA_ACT_AS_RESOURCE_ADMIN_ACCOUNT = "actAsResourceAdminAccount";

    public static final String ACCOUNT_DEACTIVATE = "account.deactivate";
    public static final String ACCOUNT_REMOVE = "account.remove";

    public static final String AUTH_TYPE = "authType";
    public static final String LAST_ADMIN_ACCOUNT = "LastAdminAccount";
    public static final String ADMIN_REQUIRED_MESSAGE = "At least one admin account is required at all times";

    public static final String ORC_KUBERNETES = "k8s";
    public static final String ORC_KUBERNETES_DISPLAY = "kubernetes";
    public static final String ORC_SWARM = "swarm";
    public static final String ORC_MESOS = "mesos";
    public static final String ORC_WINDOWS = "windows";

    public static final String STATE_PURGED = "purged";
    public static final String STATE_PURGING = "purging";

    public static final String VIRTUAL_MACHINE = "virtualMachine";
    public static final List<String> ORC_PRIORITY = Arrays.asList(
            AccountConstants.ORC_KUBERNETES,
            AccountConstants.ORC_SWARM,
            AccountConstants.ORC_MESOS,
            AccountConstants.ORC_WINDOWS
            );
    public static final Set<String> ORCS = new HashSet<>(ORC_PRIORITY);

    public static ServicesPortRange getDefaultServicesPortRange() {
        PortRangeSpec spec = new PortRangeSpec(ENV_PORT_RANGE.get());
        return new ServicesPortRange(spec.getStartPort(), spec.getEndPort());
    }

    public static String chooseOrchestration(List<String> externalIds) {
        String orchestration = "cattle";
        Set<String> installedOrcs = new HashSet<>();
        for (String externalId : externalIds) {
            String orcType = getStackTypeFromExternalId(externalId);
            if (ORCS.contains(orcType)) {
                installedOrcs.add(orcType);
            }
        }

        for (String orc : ORC_PRIORITY) {
            if (installedOrcs.contains(orc)) {
                if (AccountConstants.ORC_KUBERNETES.equals(orc)) {
                    orchestration = AccountConstants.ORC_KUBERNETES_DISPLAY;
                } else {
                    orchestration = orc;
                }
            }
        }

        return orchestration;
    }

    public static String getStackTypeFromExternalId(String externalId) {
        externalId = StringUtils.removeStart(externalId, "catalog://");
        String[] parts = externalId.split(":");
        if (parts.length < 2) {
            return null;
        }
        return StringUtils.removeStart(parts[1], "infra*");
    }

}
