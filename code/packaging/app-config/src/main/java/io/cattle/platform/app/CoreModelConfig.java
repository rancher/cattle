package io.cattle.platform.app;

import io.cattle.platform.core.addon.BlkioDeviceOption;
import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.addon.ComposeConfig;
import io.cattle.platform.core.addon.ConvertToServiceInput;
import io.cattle.platform.core.addon.HaConfigInput;
import io.cattle.platform.core.addon.HaproxyConfig;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.LogConfig;
import io.cattle.platform.core.addon.MountEntry;
import io.cattle.platform.core.addon.NetworkPolicyRule;
import io.cattle.platform.core.addon.NetworkPolicyRule.NetworkPolicyRuleAction;
import io.cattle.platform.core.addon.NetworkPolicyRule.NetworkPolicyRuleWithin;
import io.cattle.platform.core.addon.NetworkPolicyRuleBetween;
import io.cattle.platform.core.addon.NetworkPolicyRuleMember;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.addon.ProcessPool;
import io.cattle.platform.core.addon.ProcessSummary;
import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.addon.RecreateOnQuorumStrategyConfig;
import io.cattle.platform.core.addon.RestartPolicy;
import io.cattle.platform.core.addon.RollingRestartStrategy;
import io.cattle.platform.core.addon.ScalePolicy;
import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.addon.ServiceRestart;
import io.cattle.platform.core.addon.ServiceRollback;
import io.cattle.platform.core.addon.ServiceUpgrade;
import io.cattle.platform.core.addon.ServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ServicesPortRange;
import io.cattle.platform.core.addon.TargetPortRule;
import io.cattle.platform.core.addon.Ulimit;
import io.cattle.platform.core.addon.VirtualMachineDisk;
import io.cattle.platform.core.addon.VolumeActivateInput;
import io.cattle.platform.core.model.CattleTable;
import io.cattle.platform.db.jooq.utils.SchemaRecordTypeListGenerator;
import io.cattle.platform.extension.dynamic.api.addon.ExternalHandlerProcessConfig;
import io.cattle.platform.object.meta.TypeSet;
import io.cattle.platform.util.type.Priority;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreModelConfig {

    @Bean
    SchemaRecordTypeListGenerator CoreSchemaList() {
        SchemaRecordTypeListGenerator generator = new SchemaRecordTypeListGenerator();
        generator.setSchemaClass(CattleTable.class);
        return generator;
    }

    @Bean
    TypeSet CoreAddonTypeSet() {
        TypeSet typeSet = new TypeSet("CoreAddonTypeSet");
        typeSet.setTypeClasses(Arrays.asList(
                LogConfig.class,
                RestartPolicy.class,
                LoadBalancerCookieStickinessPolicy.class,
                ExternalHandlerProcessConfig.class,
                ComposeConfig.class,
                InstanceHealthCheck.class,
                ServiceLink.class,
                ServiceUpgrade.class,
                ServiceUpgradeStrategy.class,
                InServiceUpgradeStrategy.class,
                PublicEndpoint.class,
                VirtualMachineDisk.class,
                VolumeActivateInput.class,
                HaproxyConfig.class,
                RollingRestartStrategy.class,
                ServiceRestart.class,
                ServicesPortRange.class,
                RecreateOnQuorumStrategyConfig.class,
                HaConfigInput.class,
                BlkioDeviceOption.class,
                ScalePolicy.class,
                Ulimit.class,
                CatalogTemplate.class,
                PortRule.class,
                TargetPortRule.class,
                MountEntry.class,
                NetworkPolicyRule.class,
                NetworkPolicyRuleMember.class,
                NetworkPolicyRuleBetween.class,
                NetworkPolicyRuleWithin.class,
                NetworkPolicyRuleAction.class,
                ProcessSummary.class,
                ProcessPool.class,
                SecretReference.class,
                ServiceRollback.class,
                ConvertToServiceInput.class
                ));
        return typeSet;
    }

    @Bean
    TypeSet CoreTypeSet(SchemaRecordTypeListGenerator list) {
        TypeSet typeSet = new TypeSet("CoreTypeSet");
        typeSet.setTypeClasses(list.getRecordTypes());
        typeSet.setTypeNames(Arrays.asList(
                "addOutputsInput",
                "addRemoveServiceLinkInput",
                "changeSecretInput",
                "apiKey,parent=credential",
                "composeConfigInput",
                "container,parent=instance",
                "instanceConsole",
                "instanceConsoleInput",
                "instanceStop",
                "project,parent=account",
                "password,parent=credential",
                "registry,parent=storagePool",
                "registryCredential,parent=credential",
                "setProjectMembersInput",
                "setServiceLinksInput",
                "sshKey,parent=credential",
                "virtualMachine,parent=container",
                "storageDriverService,parent=service",
                "networkDriverService,parent=service",
                "loadBalancerService,parent=service",
                "externalService,parent=service",
                "dnsService,parent=service",
                "kubernetesService,parent=service",
                "launchConfig,parent=container",
                "secondaryLaunchConfig,parent=launchConfig",
                "pullTask,parent=genericObject",
                "externalVolumeEvent,parent=externalEvent",
                "externalStoragePoolEvent,parent=externalEvent",
                "externalServiceEvent,parent=externalEvent",
                "stackUpgrade",
                "kubernetesStackUpgrade",
                "externalDnsEvent,parent=externalEvent",
                "externalHostEvent,parent=externalEvent",
                "loadBalancerConfig",
                "composeService,parent=service",
                "composeProject,parent=stack",
                "kubernetesStack,parent=stack",
                "haConfig",
                "machine,parent=physicalHost",
                "revertToSnapshotInput",
                "restoreFromBackupInput",
                "snapshotBackupInput,parent=backup",
                "volumeSnapshotInput",
                "nfsConfig",
                "binding",
                "serviceBinding",
                "lbConfig",
                "lbTargetConfig",
                "balancerServiceConfig",
                "balancerTargetConfig",
                "defaultNetwork,parent=network"
                ));
        typeSet.setPriority(Priority.PRE);
        return typeSet;
    }

}
