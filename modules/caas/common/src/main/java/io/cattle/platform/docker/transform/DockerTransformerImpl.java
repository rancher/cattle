package io.cattle.platform.docker.transform;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.LxcConf;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import io.cattle.platform.core.addon.BlkioDeviceOption;
import io.cattle.platform.core.addon.LogConfig;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.addon.Ulimit;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerVolumeConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.constants.InstanceConstants.*;

public class DockerTransformerImpl implements DockerTransformer {

    private static final String HOST_CONFIG = "HostConfig";
    private static final String CONFIG = "Config";
    private static final String ACCESS_MODE = "RW";
    private static final String DRIVER = "Driver";
    private static final String DEST = "Destination";
    private static final String SRC = "Source";
    private static final String NAME = "Name";

    private static final String READ_IOPS= "BlkioDeviceReadIOps";
    private static final String WRITE_IOPS= "BlkioDeviceWriteIOps";
    private static final String READ_BPS= "BlkioDeviceReadBps";
    private static final String WRITE_BPS= "BlkioDeviceWriteBps";
    private static final String WEIGHT = "BlkioWeightDevice";

    private static final String RANCHER_VOLUME_PREFIX = "/var/lib/rancher/volumes";

    JsonMapper jsonMapper;

    public DockerTransformerImpl(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<DockerInspectTransformVolume> transformVolumes(Map<String, Object> fromInspect, List<Object> mounts) {
        return transformMounts(mounts);
    }

    @SuppressWarnings("unchecked")
    protected List<DockerInspectTransformVolume> transformMounts(List<Object> mounts) {
        if (mounts == null) {
            return null;
        }
        List<DockerInspectTransformVolume> volumes = new ArrayList<>();
        for (Object mount : mounts) {
            Map<String, Object> mountObj = (Map<String, Object>)mount;
            String am = ((boolean)mountObj.get(ACCESS_MODE)) ? DockerVolumeConstants.READ_WRITE : DockerVolumeConstants.READ_ONLY;
            String dr = (String)mountObj.get(DRIVER);
            String containerPath = (String)mountObj.get(DEST);
            String hostPath = (String)mountObj.get(SRC);
            String name = (String)mountObj.get(NAME);
            String externalId = null;
            if (StringUtils.startsWith(hostPath, RANCHER_VOLUME_PREFIX)) {
                name = Paths.get(hostPath).getFileName().toString();
                dr = Paths.get(hostPath).getParent().getFileName().toString();
            }
            if (StringUtils.isEmpty(name)) {
                name = hostPath;
            } else {
                externalId = name;
            }

            if ("rancher-cni".equals(name)) {
                continue;
            }

            if (dr == null) {
                continue;
            }

            volumes.add(new DockerInspectTransformVolume(containerPath, am, dr, name, externalId));
        }
        return volumes;
    }

    @Override
    public void transform(Map<String, Object> fromInspect, Instance instance) {
        InspectContainerResponse inspect = transformInspect(fromInspect);
        ContainerConfig containerConfig = inspect.getConfig();
        HostConfig hostConfig = inspect.getHostConfig();

        instance.setExternalId(inspect.getId());
        instance.setKind(KIND_CONTAINER);
        setName(instance, inspect, fromInspect);

        if (containerConfig != null) {
            instance.setHostname((String) fixEmptyValue(containerConfig.getHostName()));
            setField(instance, FIELD_MEMORY, containerConfig.getMemoryLimit());
            setField(instance, FIELD_CPU_SET, containerConfig.getCpuset());
            setField(instance, FIELD_CPU_SHARES, containerConfig.getCpuShares());
            setField(instance, FIELD_MEMORY_SWAP, containerConfig.getMemorySwap());
            setField(instance, FIELD_DOMAIN_NAME, containerConfig.getDomainName());
            setField(instance, FIELD_USER, containerConfig.getUser());
            setField(instance, FIELD_TTY, containerConfig.isTty());
            setField(instance, FIELD_STDIN_OPEN, containerConfig.isStdinOpen());
            setImage(instance, containerConfig.getImage());
            setField(instance, FIELD_WORKING_DIR, containerConfig.getWorkingDir());
            setEnvironment(instance, containerConfig.getEnv());
            setCommand(instance, containerConfig.getCmd());
            setListField(instance, FIELD_ENTRY_POINT, containerConfig.getEntrypoint());
            setField(instance, FIELD_VOLUME_DRIVER, fromInspect, "Config", "VolumeDriver");
            setField(instance, FIELD_STOP_SIGNAL, fromInspect, CONFIG, "StopSignal");
            setHealthConfig(fromInspect, instance);
        }

        if (containerConfig != null && hostConfig != null) {
            setVolumes(instance, containerConfig.getVolumes(), hostConfig.getBinds());
            setPorts(instance, safeGetExposedPorts(containerConfig), hostConfig.getPortBindings());
        }

        if (hostConfig != null) {
            setField(instance, FIELD_PRIVILEGED, hostConfig.isPrivileged());
            setField(instance, FIELD_PUBLISH_ALL_PORTS, hostConfig.isPublishAllPorts());
            setLxcConf(instance, hostConfig.getLxcConf());
            setListField(instance, FIELD_DNS, hostConfig.getDns());
            setListField(instance, FIELD_DNS_SEARCH, hostConfig.getDnsSearch());
            setCapField(instance, FIELD_CAP_ADD, hostConfig.getCapAdd());
            setCapField(instance, FIELD_CAP_DROP, hostConfig.getCapDrop());
            setRestartPolicy(instance, hostConfig.getRestartPolicy());
            setDevices(instance, hostConfig.getDevices());
            setMemoryReservation(fromInspect, instance);
            setField(instance, FIELD_BLKIO_WEIGHT, fromInspect, HOST_CONFIG, "BlkioWeight");
            setField(instance, FIELD_CGROUP_PARENT, fromInspect, HOST_CONFIG, "CgroupParent");
            setField(instance, FIELD_CPU_PERIOD, fromInspect, HOST_CONFIG, "CpuPeriod");
            setField(instance, FIELD_CPU_QUOTA, fromInspect, HOST_CONFIG, "CpuQuota");
            setField(instance, FIELD_CPUSET_MEMS, fromInspect, HOST_CONFIG, "CpusetMems");
            setField(instance, FIELD_DNS_OPT, fromInspect, HOST_CONFIG, "DnsOptions");
            setField(instance, FIELD_GROUP_ADD, fromInspect, HOST_CONFIG, "GroupAdd");
            setField(instance, FIELD_KERNEL_MEMORY, fromInspect, HOST_CONFIG, "KernelMemory");
            setField(instance, FIELD_MEMORY_SWAPPINESS, fromInspect, HOST_CONFIG, "MemorySwappiness");
            setField(instance, FIELD_OOMKILL_DISABLE, fromInspect, HOST_CONFIG, "OomKillDisable");
            setField(instance, FIELD_SHM_SIZE, fromInspect, HOST_CONFIG, "ShmSize");
            setField(instance, FIELD_TMPFS, fromInspect, HOST_CONFIG, "Tmpfs");
            setField(instance, FIELD_UTS, fromInspect, HOST_CONFIG, "UTSMode");
            setField(instance, FIELD_IPC_MODE, fromInspect, HOST_CONFIG, "IpcMode");
            setField(instance, FIELD_SYSCTLS, fromInspect, HOST_CONFIG, "Sysctls");
            setField(instance, FIELD_OOM_SCORE_ADJ, fromInspect, HOST_CONFIG, "OomScoreAdj");
            setField(instance, FIELD_ISOLATION, fromInspect, HOST_CONFIG, "Isolation");
        }

        setBlkioDeviceOptionss(instance, fromInspect);
        setUlimit(instance, fromInspect);
        setNetworkMode(instance, containerConfig, hostConfig);
        setField(instance, FIELD_SECURITY_OPT, fromInspect, HOST_CONFIG, "SecurityOpt");
        setField(instance, FIELD_PID_MODE, fromInspect, HOST_CONFIG, "PidMode");
        setField(instance, FIELD_READ_ONLY, fromInspect, HOST_CONFIG, "ReadonlyRootfs");
        setField(instance, FIELD_EXTRA_HOSTS, fromInspect, HOST_CONFIG, "ExtraHosts");
        setFieldIfNotEmpty(instance, FIELD_CPU_SHARES, fromInspect, HOST_CONFIG, "CpuShares");
        setFieldIfNotEmpty(instance, FIELD_CPU_SET, fromInspect, HOST_CONFIG, "CpusetCpus");
        setFieldIfNotEmpty(instance, FIELD_MEMORY, fromInspect, HOST_CONFIG, "Memory");
        setFieldIfNotEmpty(instance, FIELD_MEMORY_SWAP, fromInspect, HOST_CONFIG, "MemorySwap");
        setLogConfig(instance, fromInspect);
        setLabels(instance, fromInspect);

        // Currently not implemented: VolumesFrom, Links,
        // Consider: AttachStdin, AttachStdout, AttachStderr, StdinOnce,
    }

    private void setMemoryReservation(Map<String, Object> fromInspect, Instance instance) {
        Object memRes = CollectionUtils.getNestedValue(fromInspect, HOST_CONFIG, "MemoryReservation");
        if (memRes != null && memRes instanceof Number) {
            instance.setMemoryReservation(((Number)memRes).longValue());
        }
    }

    private void setHealthConfig(Map<String, Object> fromInspect, Instance instance) {
        Object healthCmd = CollectionUtils.getNestedValue(fromInspect, CONFIG, "Healthcheck", "Test");
        Object healthInterval = CollectionUtils.getNestedValue(fromInspect, CONFIG, "Healthcheck", "Interval");
        Object healthTimeout = CollectionUtils.getNestedValue(fromInspect, CONFIG, "Healthcheck", "Timeout");
        Object healthRetries = CollectionUtils.getNestedValue(fromInspect, CONFIG, "Healthcheck", "Retries");
        if (healthCmd != null) {
            setField(instance, "healthCmd", healthCmd);
        }
        if (healthInterval != null) {
            setField(instance, "healthInterval", healthInterval);
        }
        if (healthTimeout != null) {
            setField(instance, "healthTimeout", healthTimeout);
        }
        if (healthTimeout != null) {
            setField(instance, "healthRetries", healthRetries);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void setBlkioDeviceOptionss(Instance instance, Map<String, Object> fromInspect) {
        /*
         * We're coverting from docker's structure of:
         *  {BlkioDeviceReadIOps: [{Path: <device>, Rate: 1000} ... ], BlkioDeviceWriteIOps: [{Path: <device>, Rate: 1000} ... ] ... }
         * to cattle's structure of:
         *  {blkioDeviceOptions: {<device>: {readIops: 1000m writeIops: 1000 ... } ... }
         */
        List<String> fields = Arrays.asList(READ_IOPS, WRITE_IOPS, READ_BPS, WRITE_BPS, WEIGHT);
        Map<String, BlkioDeviceOption> target = new HashMap<>();

        for (String field : fields) {
            List<Map> deviceOptions;
            try {
                deviceOptions = (List<Map>)CollectionUtils.toList(CollectionUtils.getNestedValue(fromInspect, HOST_CONFIG, field));
            } catch (Exception e) {
                continue;
            }

            if (deviceOptions == null || deviceOptions.isEmpty()) {
                continue;
            }

            for (Map deviceOption : deviceOptions) {
                String path = null;
                Integer value = null;
                try {
                    path = (String)deviceOption.get("Path");
                    value = (Integer)(WEIGHT.equals(field) ? deviceOption.get("Weight") : deviceOption.get("Rate"));
                } catch (Exception e) {
                    // just skip it
                }
                if (path != null && value != null) {
                    BlkioDeviceOption targetDevOpt = target.computeIfAbsent(path, k -> new BlkioDeviceOption());

                    switch (field) {
                    case READ_IOPS:
                        targetDevOpt.setReadIops(value);
                        break;
                    case WRITE_IOPS:
                        targetDevOpt.setWriteIops(value);
                        break;
                    case READ_BPS:
                        targetDevOpt.setReadBps(value);
                        break;
                    case WRITE_BPS:
                        targetDevOpt.setWriteBps(value);
                        break;
                    case WEIGHT:
                        targetDevOpt.setWeight(value);
                        break;
                    }
                }
            }
        }

        if (!target.isEmpty()) {
            setField(instance, FIELD_BLKIO_DEVICE_OPTIONS, target);
        }
    }

    private void setNetworkMode(Instance instance, ContainerConfig containerConfig, HostConfig hostConfig) {
        if(DataAccessor.fields(instance).withKey(FIELD_NETWORK_MODE).get() != null)
            return;

        String netMode = null;
        if (containerConfig != null && containerConfig.isNetworkDisabled()) {
            netMode = NetworkConstants.NETWORK_MODE_NONE;
        } else if (hostConfig != null) {
            String inspectNetMode = hostConfig.getNetworkMode();
            if (NetworkConstants.NETWORK_MODE_BRIDGE.equals(inspectNetMode) ||
                    NetworkConstants.NETWORK_MODE_HOST.equals(inspectNetMode) ||
                    NetworkConstants.NETWORK_MODE_NONE.equals(inspectNetMode)) {
                netMode = inspectNetMode;
            } else if (NetworkConstants.NETWORK_MODE_DEFAULT.equals(inspectNetMode) || StringUtils.isBlank(inspectNetMode)) {
                netMode = NetworkConstants.NETWORK_MODE_BRIDGE;
            } else if (StringUtils.startsWith(inspectNetMode, NetworkConstants.NETWORK_MODE_CONTAINER)) {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                        "Transformer API does not support container network mode.", null);
            } else {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                        "Unrecognized network mode: " + inspectNetMode, null);
            }
        }

        setField(instance, FIELD_NETWORK_MODE, netMode);
    }

    private void setField(Instance instance, String field, Map<String, Object> fromInspect, String... keys) {
        Object l = CollectionUtils.getNestedValue(fromInspect, keys);
        setField(instance, field, l);
    }

    private void setFieldIfNotEmpty(Instance instance, String field, Map<String, Object> fromInspect, String... keys) {
        Object l = CollectionUtils.getNestedValue(fromInspect, keys);
        if (!isEmptyValue(l)) {
            setField(instance, field, l);
        }
    }

    @SuppressWarnings("unchecked")
    private void setLogConfig(Instance instance, Map<String, Object> fromInspect) {
        Object type = CollectionUtils.getNestedValue(fromInspect, HOST_CONFIG, "LogConfig", "Type");
        Object config = CollectionUtils.getNestedValue(fromInspect, HOST_CONFIG, "LogConfig", "Config");

        if (type == null && config == null) {
            return;
        }

        LogConfig logConfig = new LogConfig();
        if (type != null) {
            logConfig.setDriver(type.toString());
        }
        if (config instanceof Map) {
            logConfig.setConfig((Map<String, String>)config);
        }

        setField(instance, FIELD_LOG_CONFIG, logConfig);
    }

    @SuppressWarnings("unchecked")
    private void setUlimit(Instance instance, Map<String, Object> fromInspect) {
        Object ulimits = CollectionUtils.getNestedValue(fromInspect, HOST_CONFIG, "Ulimits");

        if (ulimits == null) {
            return;
        }

        List<Ulimit> ret = new ArrayList<>();
        if (ulimits instanceof List) {
            for (Object ulimit : (List<Object>) ulimits) {
                if (ulimit instanceof Map) {
                    Ulimit l = new Ulimit();
                    Map<String, Object> temp = (Map<String, Object>) ulimit;
                    if (temp.get("Name") instanceof String) {
                        l.setName(temp.get("Name").toString());
                    }
                    if (temp.get("Hard") instanceof Number) {
                        l.setHard(((Number) temp.get("Hard")).intValue());
                    }
                    if (temp.get("Soft") instanceof Integer) {
                        l.setSoft(((Number) temp.get("Soft")).intValue());
                    }
                    if (!l.getName().isEmpty()) {
                        ret.add(l);
                    }
                }
            }
        }
        if (!ret.isEmpty()) {
            setField(instance, FIELD_ULIMITS, ret);
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes" })
    public void setLabels(Instance instance, Map<String, Object> fromInspect) {
        // Labels not yet implemented in docker-java. Need to use the raw map
        Object l = CollectionUtils.getNestedValue(fromInspect, "Config", "Labels");
        Map<String, Object> cleanedLabels = new HashMap<>();
        if (l instanceof Map) {
            Map labels = (Map)l;
            for (Object key : labels.keySet()) {
                if (key == null)
                    continue;
                Object value = labels.get(key);
                if (value == null)
                    value = "";
                cleanedLabels.put(key.toString(), value.toString());
            }
        }
        Map<String, Object> labels = DataAccessor.fieldMap(instance, FIELD_LABELS);
        labels.putAll(cleanedLabels);
        setField(instance, FIELD_LABELS, labels);
    }

    private void setImage(Instance instance, String image) {
        setField(instance, FIELD_IMAGE, image);
    }

    private void setName(Instance instance, InspectContainerResponse inspect, Map<String, Object> fromInspect) {
        String name = inspect.getName();
        Object displayNameLabel = CollectionUtils.getNestedValue(fromInspect, "Config", "Labels", SystemLabels.LABEL_DISPLAY_NAME);
        if (displayNameLabel == null) {
            displayNameLabel = DataAccessor.fieldMap(instance, FIELD_LABELS).get(SystemLabels.LABEL_DISPLAY_NAME);
        }
        if (displayNameLabel != null) {
            String displayName = displayNameLabel.toString();
            if (StringUtils.isNotBlank(displayName)) {
                name = displayName;
            }
        }

        if (name != null) {
            name = name.replaceFirst("/", "");
            instance.setName(name);
        }
    }

    private ExposedPort[] safeGetExposedPorts(ContainerConfig containerConfig) {
        try {
            return containerConfig.getExposedPorts();
        } catch (NullPointerException e) {
            // Bug in docker-java doesn't account for this property being null
            return null;
        }
    }

    private void setDevices(Instance instance, Device[] devices) {
        if (devices == null) {
            devices = new Device[0];
        }

        List<String> instanceDevices = new ArrayList<>();
        for (Device d : devices) {
            StringBuilder fullDevice = new StringBuilder(d.getPathOnHost()).append(":").append(d.getPathInContainer()).append(":");
            if (StringUtils.isEmpty(d.getcGroupPermissions())) {
                fullDevice.append("rwm");
            } else {
                fullDevice.append(d.getcGroupPermissions());
            }
            instanceDevices.add(fullDevice.toString());
        }

        setField(instance, FIELD_DEVICES, instanceDevices);
    }

    private void setRestartPolicy(Instance instance, RestartPolicy restartPolicy) {
        if (restartPolicy == null || StringUtils.isEmpty(restartPolicy.getName())) {
            return;
        }

        io.cattle.platform.core.addon.RestartPolicy rp = new io.cattle.platform.core.addon.RestartPolicy();
        rp.setMaximumRetryCount(restartPolicy.getMaximumRetryCount());
        rp.setName(restartPolicy.getName());
        setField(instance, FIELD_RESTART_POLICY, rp);
    }

    private void setCapField(Instance instance, String field, Capability[] caps) {
        if (caps == null) {
            caps = new Capability[0];
        }

        List<String> list = new ArrayList<>();
        for (Capability cap : caps) {
            list.add(cap.toString());
        }
        setField(instance, field, list);
    }

    private void setLxcConf(Instance instance, LxcConf[] lxcConf) {
        if (lxcConf == null || lxcConf.length == 0) {
            lxcConf = new LxcConf[0];
        }

        Map<String, String> instanceLxcConf = new HashMap<>();
        for (LxcConf lxc : lxcConf) {
            instanceLxcConf.put(lxc.getKey(), lxc.getValue());
        }

        setField(instance, FIELD_LXC_CONF, instanceLxcConf);
    }

    private void setPorts(Instance instance, ExposedPort[] exposedPorts, Ports portBindings) {
        if (exposedPorts == null || exposedPorts.length == 0) {
            return;
        }

        List<PortInstance> containerPortInstances = new ArrayList<>();
        List<String> ports = new ArrayList<>();
        for (ExposedPort ep : exposedPorts) {
            Binding[] bindings = portBindings == null || portBindings.getBindings() == null ? null : portBindings.getBindings().get(ep);
            if (bindings != null && bindings.length > 0) {
                for (Binding b : bindings) {
                    PortInstance portInstance = newPortInstance(ep);
                    // HostPort should really be a string, not an int.  Somehow empty string becomes 0
                    if (b.getHostPort() != null && b.getHostPort() != 0) {
                        portInstance.setPublicPort(b.getHostPort());
                    }
                    if (StringUtils.isNotBlank(b.getHostIp())) {
                        portInstance.setIpAddress(b.getHostIp());
                        portInstance.setBindIpAddress(b.getHostIp());
                    }
                    containerPortInstances.add(portInstance);
                }
            } else {
                containerPortInstances.add(newPortInstance(ep));
            }
        }

        List<PortSpec> portSpecs = InstanceConstants.getPortSpecs(instance);
        List<String> portSpecStrings = new ArrayList<>(DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_PORTS, String.class));
        for (PortInstance portInstance : containerPortInstances) {
            boolean found = false;
            for (PortSpec portSpec : portSpecs) {
                if (portInstance.matches(portSpec)) {
                    portSpec.populate(portInstance);
                    found = true;
                    break;
                }
            }

            if (!found) {
                portSpecStrings.add(new PortSpec(portInstance).toSpec());
            }
        }
        setField(instance, FIELD_PORTS, portSpecs);

        boolean changed = false;
        List<PortInstance> portInstances = DataAccessor.fieldObjectList(instance, FIELD_PORT_BINDINGS, PortInstance.class);
        for (PortInstance containerPortInstance : portInstances) {
            PortSpec spec = new PortSpec(containerPortInstance);
            boolean found = false;
            for (PortInstance portInstance : portInstances) {
                if (portInstance.matches(spec)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                portInstances.add(containerPortInstance);
                changed = true;
            }
        }

        if (changed) {
            setField(instance, FIELD_PORT_BINDINGS, ports);
        }
    }

    private PortInstance newPortInstance(ExposedPort ep) {
        return new PortInstance(ep.getPort(), ep.getProtocol() == null ? "tcp" : ep.getProtocol().toString());
    }

    private void setVolumes(Instance instance, Map<String, ?> volumes, String[] binds) {
        List<String> dataVolumes = new ArrayList<>();
        if (volumes != null) {
            dataVolumes.addAll(volumes.keySet());
        }

        if (binds != null) {
            dataVolumes.addAll(Arrays.asList(binds));
        }

        setField(instance, InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
    }

    private void setListField(Instance instance, String field, String[] value) {
        if (value == null) {
            value = new String[0];
        }

        List<String> list = new ArrayList<>(Arrays.asList(value));
        setField(instance, field, list);
    }

    private void setCommand(Instance instance, String[] cmd) {
        if (cmd == null) {
            cmd = new String[0];
        }

        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(cmd));
        setField(instance, FIELD_COMMAND, args);
    }

    private void setEnvironment(Instance instance, String[] env) {
        if (env == null) {
            env = new String[0];
        }

        Map<String, String> envMap = new HashMap<>();
        for (String e : env) {
            String[] kvp = e.split("=", 2);
            if (kvp.length == 2) {
                envMap.put(kvp[0], kvp[1]);
            } else if (kvp.length == 1) {
                envMap.put(kvp[0], "");
            }
        }
        setField(instance, FIELD_ENVIRONMENT, envMap);
    }

    private void setField(Instance instance, String field, Object fieldValue) {
        fieldValue = fixEmptyValue(fieldValue);
        DataAccessor.fields(instance).withKey(field).set(fieldValue);
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    private Object fixEmptyValue(Object fieldValue) {
        if (fieldValue instanceof String && StringUtils.isEmpty((String)fieldValue)) {
            return null;
        } else if (fieldValue instanceof Number && ((Number)fieldValue).longValue() == 0L) {
            return null;
        }
        return fieldValue;
    }

    private boolean isEmptyValue(Object fieldValue) {
        return fieldValue == null ||
                (fieldValue instanceof String && StringUtils.isEmpty((String) fieldValue)) ||
                (fieldValue instanceof Number && ((Number) fieldValue).longValue() == 0L);
    }

    private InspectContainerResponse transformInspect(Map<String, Object> inspect) {
        return jsonMapper.convertValue(inspect, InspectContainerResponse.class);
    }

    @Override
    public int getExitCode(Instance instance) {
        Object obj = CollectionUtils.getNestedValue(instance.getData(),
                DataAccessor.FIELDS,
                FIELD_DOCKER_INSPECT,
                "State",
                "ExitCode");
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj == null) {
            Integer fieldCode = DataAccessor.fieldInteger(instance, InstanceConstants.FIELD_EXIT_CODE);
            if (fieldCode != null) {
                return fieldCode;
            }
        }
        return 0;
    }
}
