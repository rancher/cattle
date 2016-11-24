package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.SettingTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.core.model.Stack;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.List;

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

}
