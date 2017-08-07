package io.cattle.platform.app.components;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.hostapi.HostApiProxyTokenImpl;
import io.cattle.platform.api.pubsub.model.Publish;
import io.cattle.platform.api.pubsub.model.Subscribe;
import io.cattle.platform.api.stats.StatsAccess;
import io.cattle.platform.core.addon.BaseMachineConfig;
import io.cattle.platform.core.addon.BlkioDeviceOption;
import io.cattle.platform.core.addon.ClusterIdentity;
import io.cattle.platform.core.addon.ComposeConfig;
import io.cattle.platform.core.addon.ContainerEvent;
import io.cattle.platform.core.addon.ContainerUpgrade;
import io.cattle.platform.core.addon.DependsOn;
import io.cattle.platform.core.addon.DeploymentSyncRequest;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.addon.HaproxyConfig;
import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.InstanceStatus;
import io.cattle.platform.core.addon.Link;
import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.LogConfig;
import io.cattle.platform.core.addon.MountEntry;
import io.cattle.platform.core.addon.NetworkPolicyRule;
import io.cattle.platform.core.addon.NetworkPolicyRule.NetworkPolicyRuleAction;
import io.cattle.platform.core.addon.NetworkPolicyRule.NetworkPolicyRuleWithin;
import io.cattle.platform.core.addon.NetworkPolicyRuleBetween;
import io.cattle.platform.core.addon.NetworkPolicyRuleMember;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.addon.ProcessPool;
import io.cattle.platform.core.addon.ProcessSummary;
import io.cattle.platform.core.addon.RestartPolicy;
import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.addon.ServiceRollback;
import io.cattle.platform.core.addon.ServiceUpgrade;
import io.cattle.platform.core.addon.ServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ServicesPortRange;
import io.cattle.platform.core.addon.SetComputeFlavorInput;
import io.cattle.platform.core.addon.StackConfiguration;
import io.cattle.platform.core.addon.TargetPortRule;
import io.cattle.platform.core.addon.Ulimit;
import io.cattle.platform.core.addon.VirtualMachineDisk;
import io.cattle.platform.core.addon.VolumeActivateInput;
import io.cattle.platform.core.model.CattleTable;
import io.cattle.platform.db.jooq.utils.SchemaRecordTypeListGenerator;
import io.cattle.platform.docker.api.model.ContainerExec;
import io.cattle.platform.docker.api.model.ContainerLogs;
import io.cattle.platform.docker.api.model.ContainerProxy;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.api.model.ServiceProxy;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureConfig;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPConfig;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADConfig;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthConfig;
import io.cattle.platform.iaas.api.auth.projects.Member;
import io.cattle.platform.object.meta.TypeSet;
import io.github.ibuildthecloud.gdapi.doc.FieldDocumentation;
import io.github.ibuildthecloud.gdapi.doc.TypeDocumentation;

import java.util.Arrays;

public class Model {

    Framework f;

    public Model(Framework framework) {
        this.f = framework;
        init();
    }

    private void init() {
        setupTypes();
        setupProcessDefinitionsTemplates();
        setupProcessDefinitions();
        f.wireUpTypes();
    }

    private void setupProcessDefinitionsTemplates() {
        // This established the default behavior of each process
        f.processBuilder
                .blacklist("requested", "removing", "removed", "purging", "purged")
                .template("create")
                    .from("requested")
                    .transitioning("creating")
                    .to("active")
                    .ifProcessExistThenTo("activate", "inactive")
                .template("activate")
                    .from("inactive", "error", "paused")
                    .transitioning("activating")
                    .to("active")
                .template("deactivate")
                    .notAfter("create", "error")
                    .transitioning("deactivating")
                    .to("inactive")
                .template("remove")
                    .transitioning("removing")
                    .to("removed")
                .template("error")
                    .transitioning("erroring")
                    .to("error")
                .template("cancelupgrade")
                    .from("upgrading")
                    .transitioning("pausing")
                    .to("paused")
                .template("finishupgrade")
                    .from("upgraded")
                    .transitioning("finishing-upgrade")
                    .to("active")
                .template("pause")
                    .transitioning("pausing")
                    .to("paused")
                .template("restart")
                    .from("active", "paused")
                    .transitioning("restarting")
                    .to("active")
                .template("rollback")
                    .transitioning("rolling-back")
                    .to("active")
                .template("update")
                    .fromResting()
                    .fromSelf()
                    .during("upgrade", "rollback")
                    .transitioning("updating")
                    .to("active")
                .template("upgrade")
                    .fromResting()
                    .fromSelf()
                    .during("update", "rollback")
                    .transitioning("upgrading")
                    .to("active")
                .template("reactivate")
                    .from("active")
                    .transitioning("activating")
                    .to("active");
    }

    private void setupProcessDefinitions() {
        f.processBuilder
                // NOTE: all types automatically get "create" and "remove" process

                // Simple types
                .type("certificate").processes("update")
                .type("cluster")
                .type("credential").processes("activate", "deactivate")
                .type("deploymentUnit").processes("activate", "deactivate", "error", "update", "pause")
                .type("dynamicSchema")
                .type("externalevent")
                .type("genericObject")
                .type("hostTemplate")
                .type("machineDriver").processes("activate", "deactivate", "reactivate", "error", "update")
                .type("network")
                .type("networkDriver").processes("activate", "deactivate", "update")
                .type("projectMember").processes("activate", "deactivate")
                .type("secret")
                .type("serviceevent")
                .type("stack").processes("error", "pause", "rollback", "update")
                .type("storageDriver").processes("activate", "deactivate", "update")
                .type("storagePool").processes("activate", "deactivate")
                .type("subnet")
                .type("volume").processes("activate", "deactivate", "update")
                .type("volumeTemplate")

                // Now the more complicated ones
                .type("account")
                    .process("activate")
                    .process("deactivate")
                    .process("purge")
                        .from("removed")
                        .transitioning("purging")
                        .to("purged")

                .type("agent")
                    .process("activate")
                    .process("deactivate")
                    .process("error")
                    .process("reconnect")
                        .from("disconnected", "active")
                        .during("activate", "disconnect")
                        .transitioning("reconnecting")
                        .to("active")
                    .process("disconnect")
                        .from("active")
                        .during("reconnect")
                        .transitioning("disconnecting")
                        .to("disconnected")

                .type("host")
                    .process("activate")
                    .process("update")
                    .process("deactivate")
                    .process("error")
                    .process("provision")
                        .from("creating")
                        .transitioning("provisioning")
                        .to("inactive")

                .type("mount")
                    .process("create")
                        .transitioning("activating")
                        .to("active")
                    .process("deactivate").reset()
                        .from("active")
                        .transitioning("deactivating")
                        .to("inactive")
                    .process("remove")

                .type("scheduledUpgrade")
                    .process("create")
                        .transitioning("scheduling")
                        .to("scheduled")
                    .process("start")
                        .from("scheduled")
                        .transitioning("running")
                        .to("done")

                .type("service")
                    .process("activate")
                    .process("cancelupgrade")
                    .process("deactivate")
                    .process("error")
                    .process("finishupgrade")
                    .process("pause")
                    .process("restart")
                    .process("rollback")
                    .process("update")
                    .process("upgrade")

                .type("instance")
                    .process("create")
                        .to("stopped")
                    .process("start")
                        .from("stopped")
                        .transitioning("starting")
                        .to("running")
                    .process("restart")
                        .from("running")
                        .transitioning("restarting")
                        .to("running")
                    .process("stop")
                        .from("running")
                        .during("start")
                        .transitioning("stopping")
                        .to("stopped")
                    .process("error")

                // And end!
                .build();

        f.metaDataManager.getProcessDefinitions().addAll(f.processDefinitions.values());
    }

    private void setupTypes() {
        f.metaDataManager.setTypeSets(Arrays.asList(
                databaseObjects(),
                addons(),
                named()));
    }

    private TypeSet databaseObjects() {
        SchemaRecordTypeListGenerator generator = new SchemaRecordTypeListGenerator();
        generator.setSchemaClass(CattleTable.class);

        return TypeSet.ofClasses(generator.getRecordTypes());
    }

    private TypeSet addons() {
        return TypeSet.ofClasses(
                ADConfig.class,
                AzureConfig.class,
                BaseMachineConfig.class,
                BlkioDeviceOption.class,
                ClusterIdentity.class,
                ComposeConfig.class,
                ContainerExec.class,
                ContainerEvent.class,
                ContainerLogs.class,
                ContainerProxy.class,
                ContainerUpgrade.class,
                DependsOn.class,
                DeploymentSyncRequest.class,
                DeploymentSyncResponse.class,
                FieldDocumentation.class,
                HaproxyConfig.class,
                HealthcheckState.class,
                HostAccess.class,
                HostApiProxyTokenImpl.class,
                Identity.class,
                InServiceUpgradeStrategy.class,
                InstanceHealthCheck.class,
                InstanceStatus.class,
                Link.class,
                LoadBalancerCookieStickinessPolicy.class,
                LocalAuthConfig.class,
                LogConfig.class,
                Member.class,
                MountEntry.class,
                NetworkPolicyRuleAction.class,
                NetworkPolicyRuleBetween.class,
                NetworkPolicyRule.class,
                NetworkPolicyRuleMember.class,
                NetworkPolicyRuleWithin.class,
                OpenLDAPConfig.class,
                PortInstance.class,
                PortRule.class,
                ProcessPool.class,
                ProcessSummary.class,
                Publish.class,
                RestartPolicy.class,
                SecretReference.class,
                ServiceProxy.class,
                ServiceRollback.class,
                ServicesPortRange.class,
                ServiceUpgrade.class,
                ServiceUpgradeStrategy.class,
                SetComputeFlavorInput.class,
                StackConfiguration.class,
                StatsAccess.class,
                Subscribe.class,
                TargetPortRule.class,
                Token.class,
                TypeDocumentation.class,
                Ulimit.class,
                VirtualMachineDisk.class,
                VolumeActivateInput.class);
    }

    private TypeSet named() {
        return TypeSet.ofNames(
                "addOutputsInput",
                "apiKey,parent=credential",
                "changeSecretInput",
                "composeConfigInput",
                "containerConfig,parent=container",
                "container,parent=instance",
                "defaultNetwork,parent=network",
                "dnsService,parent=service",
                "externalDnsEvent,parent=externalEvent",
                "externalHostEvent,parent=externalEvent",
                "externalServiceEvent,parent=externalEvent",
                "externalService,parent=service",
                "instanceConsole",
                "instanceConsoleInput",
                "instanceRemove",
                "instanceStop",
                "launchConfig,parent=container",
                "lbConfig",
                "lbTargetConfig",
                "loadBalancerService,parent=service",
                "networkDriverService,parent=service",
                "password,parent=credential",
                "project,parent=account",
                "pullTask,parent=genericObject",
                "register,parent=genericObject",
                "registrationToken,parent=credential",
                "registryCredential,parent=credential",
                "registry,parent=storagePool",
                "scalingGroup,parent=service",
                "selectorService,parent=service",
                "setProjectMembersInput",
                "stackUpgrade",
                "storageDriverService,parent=service",
                "virtualMachine,parent=container");
    }

}
