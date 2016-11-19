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

    @Override
    protected String getName() {
        return "sampleDataVersion13";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        updateSetting();
        updateStackExternalIds();
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

    protected void updateStackExternalIds() {
        for (String orc : new String[] {"kubernetes", "k8s", "mesos", "swarm"}) {
            String fromLike = String.format("%%catalog://library:%s:%%", orc);
            for (Stack stack : objectManager.find(Stack.class, STACK.EXTERNAL_ID,
                    new Condition(ConditionType.LIKE, fromLike), STACK.REMOVED, null)) {
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

}