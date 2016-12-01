package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.ProcessInstanceTable.*;
import static io.cattle.platform.core.model.tables.SettingTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.MachineConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.ProcessInstance;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class SampleDataStartupV13 extends AbstractSampleData {

    private static final String[] COMMUNITY_STACKS = new String[] {
                "bind9",
                "cloudflare",
                "dnsimple",
                "dnsupdate-rfc2136",
                "pointhq",
                "powerdns-external-dns"
    };

    private static final String[] LIBRARY_STACKS = new String[] {
        "kubernetes",
        "k8s",
        "mesos",
        "swarm",
        "route53"
    };

    private static final String[] STACK_PROCESSES = new String[] {
            ".cancelupgrade",
            ".create",
            ".error",
            ".finishupgrade",
            ".remove",
            ".rollback",
            ".update",
            ".upgrade",
    };

    private static final String EC2_CONFIG = "amazonec2Config";
    private static final String SECURITY_GROUP = "securityGroup";

    @Override
    protected String getName() {
        return "sampleDataVersion13";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        updateSetting();
        migrateLibraryStacks();
        migrateCommunityStacks();
        migrateMesos();

        deleteHAEnv();
        migrateProcesses();
        migrateHosts();
    }

    protected void migrateHosts() {
        for (PhysicalHost physicalHost : objectManager.find(PhysicalHost.class,
                ObjectMetaDataManager.REMOVED_FIELD, null,
                ObjectMetaDataManager.KIND_FIELD, MachineConstants.KIND_MACHINE)) {
            Map<String, Object> data = DataAccessor.fieldMap(physicalHost, EC2_CONFIG);
            if (data != null && data.containsKey(SECURITY_GROUP)) {
                Object value = data.get(SECURITY_GROUP);
                if (value instanceof String) {
                    List<Object> o = Arrays.asList(value);
                    data.put(SECURITY_GROUP, o);
                    DataAccessor.setField(physicalHost, EC2_CONFIG, data);
                    objectManager.persist(physicalHost);
                }
            }
        }
    }

    protected void updateSetting() {
        Setting setting = objectManager.findAny(Setting.class,
                SETTING.NAME, "catalog.url");
        if (setting != null) {
            String value = setting.getValue();
            if (value == null) {
                value = "";
            }

            value = value.replaceAll("https://github.com/rancher/rancher-catalog(.git)?",
                        "https://git.rancher.io/rancher-catalog.git")
                    .replaceAll("https://github.com/rancher/community-catalog(.git)?",
                            "https://git.rancher.io/community-catalog.git");
            setting.setValue(value);
            objectManager.persist(setting);
            ArchaiusUtil.refresh();
        }

        setting = objectManager.findAny(Setting.class,
                SETTING.NAME, "api.auth.ldap.openldap.group.dn.field");
        if (setting == null) {
            Setting ldapSetting = objectManager.newRecord(Setting.class);
            ldapSetting.setName("api.auth.ldap.openldap.group.dn.field");
            ldapSetting.setValue("entryDN");
            ldapSetting = objectManager.create(ldapSetting);
            ArchaiusUtil.refresh();
        }

        setting = objectManager.findAny(Setting.class,
                SETTING.NAME, "api.auth.ldap.openldap.group.member.user.attribute");
        if (setting == null) {
            Setting secondLdapSetting = objectManager.newRecord(Setting.class);
            secondLdapSetting.setName("api.auth.ldap.openldap.group.member.user.attribute");
            secondLdapSetting.setValue("entryDN");
            secondLdapSetting = objectManager.create(secondLdapSetting);
            ArchaiusUtil.refresh();
        }
    }

    protected void migrateMesos() {
        for (Stack stack : objectManager.find(Stack.class,
                STACK.EXTERNAL_ID, "system://mesos",
                STACK.REMOVED, null)) {
            stack.setExternalId("catalog://community:infra*mesos:0");
            objectManager.persist(stack);
        }
    }

    protected void migrateLibraryStacks() {
        for (String orc : LIBRARY_STACKS) {
            String fromLike = String.format("%%catalog://library:%s:%%", orc);
            for (Stack stack : objectManager.find(Stack.class,
                    STACK.EXTERNAL_ID, new Condition(ConditionType.LIKE, fromLike),
                    STACK.REMOVED, null)) {
                String[] parts = stack.getExternalId().split(":");
                String toOrc = orc;
                if (orc.equals("kubernetes")) {
                    toOrc = "k8s";
                }

                String to = String.format("catalog://library:infra*%s:%s", toOrc, parts[parts.length-1]);
                if (to.equals("catalog://library:infra*k8s:7")) {
                    to = "catalog://library:infra*k8s:8";
                }

                stack.setExternalId(to);
                objectManager.persist(stack);
            }
        }
    }

    protected void migrateCommunityStacks() {
        for (String orc : COMMUNITY_STACKS) {
            String fromLike = String.format("%%catalog://community:%s:%%", orc);
            for (Stack stack : objectManager.find(Stack.class,
                    STACK.EXTERNAL_ID, new Condition(ConditionType.LIKE, fromLike),
                    STACK.REMOVED, null)) {
                String[] parts = stack.getExternalId().split(":");
                String to = String.format("catalog://community:infra*%s:%s", orc, parts[parts.length-1]);
                stack.setExternalId(to);
                objectManager.persist(stack);
            }
        }
    }

    protected void deleteHAEnv() {
        for (Account account : objectManager.find(Account.class,
                ObjectMetaDataManager.UUID_FIELD, new Condition(ConditionType.LIKE, "system-ha-%"),
                ObjectMetaDataManager.REMOVED_FIELD, null)) {
            processManager.scheduleStandardChainedProcessAsync(StandardProcess.DEACTIVATE, StandardProcess.REMOVE,
                    account, null);
        }
    }

    protected void migrateProcesses() {
        for (String process : STACK_PROCESSES) {
            for (ProcessInstance pi : objectManager.find(ProcessInstance.class,
                    PROCESS_INSTANCE.PROCESS_NAME, "environment" + process,
                    PROCESS_INSTANCE.END_TIME, null)) {
                pi.setProcessName("stack" + process);
                pi.setResourceType("stack");
                objectManager.persist(pi);
            }
        }
    }

}
