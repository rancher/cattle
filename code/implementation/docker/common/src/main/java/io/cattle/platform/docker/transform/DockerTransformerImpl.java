package io.cattle.platform.docker.transform;

import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.docker.constants.DockerInstanceConstants.*;
import static io.cattle.platform.docker.constants.DockerNetworkConstants.*;
import io.cattle.platform.core.addon.LogConfig;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

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
import com.github.dockerjava.api.model.VolumeBind;

public class DockerTransformerImpl implements DockerTransformer {

    private static final String IMAGE_PREFIX = "docker:";
    private static final String IMAGE_KIND_PATTERN = "^(sim|docker):.*";
    private static final String READ_WRITE = "rw";
    private static final String READ_ONLY = "ro";
    private static final String MOUNTS = "Mounts";
    private static final String ACCESS_MODE = "RW";
    private static final String DRIVER = "Driver";
    private static final String DEST = "Destination";
    private static final String SRC = "Source";
    private static final String LOCAL = "local";
    private static final String NAME = "Name";

    @Inject
    JsonMapper jsonMapper;

    @Override
    @SuppressWarnings("unchecked")
    public List<DockerInspectTransformVolume> transformVolumes(Map<String, Object> fromInspect) {
        List<DockerInspectTransformVolume> volumes = transformMounts(fromInspect);
        if (volumes.size() > 0) {
            return volumes;
        }
        InspectContainerResponse inspect = transformInspect(fromInspect);
        HostConfig hostConfig = inspect.getHostConfig();
        VolumeBind[] volumeBinds = null;
        try {
            volumeBinds = inspect.getVolumes();
        } catch (NullPointerException e) {
            // A bug in docker-java can cause this.
            volumeBinds = new VolumeBind[0];
        }
        Set<String> binds = bindSet(hostConfig.getBinds());
        Map<String, String> rw = rwMap((Map<String, Boolean>) fromInspect.get("VolumesRW"));
        for (VolumeBind vb : volumeBinds) {
            String am = rw.containsKey(vb.getContainerPath()) ? rw.get(vb.getContainerPath()) : READ_WRITE;
            boolean isBindMound = binds.contains(vb.getContainerPath());
            volumes.add(new DockerInspectTransformVolume(vb.getContainerPath(), vb.getHostPath(), am, isBindMound, ""));
        }
        return volumes;
    }

    @SuppressWarnings("unchecked")
    protected List<DockerInspectTransformVolume> transformMounts(Map<String, Object> fromInspect) {
        List<DockerInspectTransformVolume> volumes = new ArrayList<DockerInspectTransformVolume>();
        List<Object> mounts = (List<Object>) fromInspect.get(MOUNTS);
        if (mounts != null) {
            for (Object mount : mounts) {
                Map<String, Object> mountObj = (Map<String, Object>) mount;
                String am = ((boolean) mountObj.get(ACCESS_MODE)) ? "rw" : "ro";
                String dr = (String) mountObj.get(DRIVER);
                String containerPath = (String) mountObj.get(DEST);
                String hostPath = (String) mountObj.get(SRC);
                String name = (String) mountObj.get(NAME);
                boolean isBindMount = (dr == null);
                if (StringUtils.isNotEmpty(dr) && !StringUtils.equals(dr, LOCAL)) {
                    hostPath = String.format("%s:///%s", dr, name);
                }
                volumes.add(new DockerInspectTransformVolume(containerPath, hostPath, am, isBindMount, dr));
            }
        }
        return volumes;
    }

    Map<String, String> rwMap(Map<String, Boolean> volumeRws) {
        // TODO When this bug is fixed, switch to using java-docker's volumesRW
        // https://github.com/docker-java/docker-java/issues/205
        Map<String, String> rwMap = new HashMap<String, String>();

        if (volumeRws == null) {
            return rwMap;
        }

        for (Map.Entry<String, Boolean> volume : volumeRws.entrySet()) {
            Boolean readWrite = volume.getValue();
            String perms = readWrite ? READ_WRITE : READ_ONLY;
            rwMap.put(volume.getKey(), perms);
        }

        return rwMap;
    }

    Set<String> bindSet(String[] binds) {
        Set<String> hostBindMounts = new HashSet<String>();
        if (binds == null)
            return hostBindMounts;

        for (String bindMount : binds) {
            String[] parts = bindMount.split(":");
            hostBindMounts.add(parts[1]);
        }
        return hostBindMounts;
    }

    @Override
    public void transform(Map<String, Object> fromInspect, Instance instance) {
        InspectContainerResponse inspect = transformInspect(fromInspect);
        ContainerConfig containerConfig = inspect.getConfig();
        HostConfig hostConfig = inspect.getHostConfig();

        instance.setExternalId(inspect.getId());
        instance.setKind(KIND_CONTAINER);
        setName(instance, inspect);

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
        }

        setNetworkMode(instance, containerConfig, hostConfig);
        setField(instance, FIELD_SECURITY_OPT, fromInspect, "HostConfig", "SecurityOpt");
        setField(instance, FIELD_PID_MODE, fromInspect, "HostConfig", "PidMode");
        setField(instance, FIELD_READ_ONLY, fromInspect, "HostConfig", "ReadonlyRootfs");
        setField(instance, FIELD_EXTRA_HOSTS, fromInspect, "HostConfig", "ExtraHosts");
        setFieldIfNotEmpty(instance, FIELD_CPU_SHARES, fromInspect, "HostConfig", "CpuShares");
        setFieldIfNotEmpty(instance, FIELD_CPU_SET, fromInspect, "HostConfig", "CpusetCpus");
        setFieldIfNotEmpty(instance, FIELD_MEMORY, fromInspect, "HostConfig", "Memory");
        setFieldIfNotEmpty(instance, FIELD_MEMORY_SWAP, fromInspect, "HostConfig", "MemorySwap");
        setLogConfig(instance, fromInspect);
        setLabels(instance, fromInspect);

        // Currently not implemented: VolumesFrom, Links,
        // Consider: AttachStdin, AttachStdout, AttachStderr, StdinOnce,
    }

    void setNetworkMode(Instance instance, ContainerConfig containerConfig, HostConfig hostConfig) {
        if(DataAccessor.fields(instance).withKey(FIELD_NETWORK_MODE).get() != null)
            return;

        String netMode = null;
        if (containerConfig != null && containerConfig.isNetworkDisabled()) {
            netMode = NETWORK_MODE_NONE;
        } else if (hostConfig != null) {
            String inspectNetMode = hostConfig.getNetworkMode();
            if (NETWORK_MODE_BRIDGE.equals(inspectNetMode) ||
                    NETWORK_MODE_HOST.equals(inspectNetMode) ||
                    NETWORK_MODE_NONE.equals(inspectNetMode)) {
                netMode = inspectNetMode;
            } else if (NETWORK_MODE_DEFAULT.equals(inspectNetMode) || StringUtils.isBlank(inspectNetMode)) {
                netMode = NETWORK_MODE_BRIDGE;
            } else if (StringUtils.startsWith(inspectNetMode, NETWORK_MODE_CONTAINER)) {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                        "Transformer API does not support container network mode.", null);
            } else {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION, 
                        "Unrecognized network mode: " + inspectNetMode, null);
            }
        }

        setField(instance, FIELD_NETWORK_MODE, netMode);
    }

    void setField(Instance instance, String field, Map<String, Object> fromInspect, String... keys) {
        Object l = CollectionUtils.getNestedValue(fromInspect, keys);
        setField(instance, field, l);
    }

    void setFieldIfNotEmpty(Instance instance, String field, Map<String, Object> fromInspect, String... keys) {
        Object l = CollectionUtils.getNestedValue(fromInspect, keys);
        if (!isEmptyValue(l)) {
            setField(instance, field, l);
        }
    }

    @SuppressWarnings("unchecked")
    void setLogConfig(Instance instance, Map<String, Object> fromInspect) {
        Object type = CollectionUtils.getNestedValue(fromInspect, "HostConfig", "LogConfig", "Type");
        Object config = CollectionUtils.getNestedValue(fromInspect, "HostConfig", "LogConfig", "Config");

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

    @SuppressWarnings({ "rawtypes" })
    void setLabels(Instance instance, Map<String, Object> fromInspect) {
        // Labels not yet implemented in docker-java. Need to use the raw map
        Object l = CollectionUtils.getNestedValue(fromInspect, "Config", "Labels");
        Map<String, String> cleanedLabels = new HashMap<String, String>();
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
        setField(instance, FIELD_LABELS, cleanedLabels);
    }

    void setImage(Instance instance, String image) {
        if (StringUtils.isNotBlank(image) && !image.matches(IMAGE_KIND_PATTERN)) {
            image = IMAGE_PREFIX + image;
        }

        setField(instance, FIELD_IMAGE_UUID, image);
    }

    void setName(Instance instance, InspectContainerResponse inspect) {
        String name = inspect.getName();
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

    void setDevices(Instance instance, Device[] devices) {
        if (devices == null) {
            devices = new Device[0];
        }

        List<String> instanceDevices = new ArrayList<String>();
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

    void setRestartPolicy(Instance instance, RestartPolicy restartPolicy) {
        if (restartPolicy == null || StringUtils.isEmpty(restartPolicy.getName())) {
            return;
        }

        io.cattle.platform.core.addon.RestartPolicy rp = new io.cattle.platform.core.addon.RestartPolicy();
        rp.setMaximumRetryCount(restartPolicy.getMaximumRetryCount());
        rp.setName(restartPolicy.getName());
        setField(instance, FIELD_RESTART_POLICY, rp);
    }

    void setCapField(Instance instance, String field, Capability[] caps) {
        if (caps == null) {
            caps = new Capability[0];
        }

        List<String> list = new ArrayList<String>();
        for (Capability cap : caps) {
            list.add(cap.toString());
        }
        setField(instance, field, list);
    }

    void setLxcConf(Instance instance, LxcConf[] lxcConf) {
        if (lxcConf == null || lxcConf.length == 0) {
            lxcConf = new LxcConf[0];
        }

        Map<String, String> instanceLxcConf = new HashMap<String, String>();
        for (LxcConf lxc : lxcConf) {
            instanceLxcConf.put(lxc.getKey(), lxc.getValue());
        }

        setField(instance, FIELD_LXC_CONF, instanceLxcConf);
    }

    void setPorts(Instance instance, ExposedPort[] exposedPorts, Ports portBindings) {
        if (exposedPorts == null) {
            exposedPorts = new ExposedPort[0];
        }

        List<String> ports = new ArrayList<String>();
        for (ExposedPort ep : exposedPorts) {
            String port = ep.toString();

            Binding[] bindings = portBindings == null || portBindings.getBindings() == null ? null : portBindings.getBindings().get(ep);
            if (bindings != null && bindings.length > 0) {
                for (Binding b : bindings) {
                    // HostPort should really be a string, not an int.  Somehow empty string becomes 0
                    if (b.getHostPort() != null && b.getHostPort() != 0) {
                        String fullPort = b.getHostPort() + ":" + port;
                        ports.add(fullPort);
                    } else {
                        ports.add(port);
                    }
                }
            } else {
                ports.add(port);
            }
        }

        setField(instance, FIELD_PORTS, ports);
    }

    void setVolumes(Instance instance, Map<String, ?> volumes, String[] binds) {
        List<String> dataVolumes = new ArrayList<String>();
        if (volumes != null) {
            dataVolumes.addAll(volumes.keySet());
        }

        if (binds != null) {
            dataVolumes.addAll(Arrays.asList(binds));
        }

        setField(instance, InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
    }

    void setListField(Instance instance, String field, String[] value) {
        if (value == null) {
            value = new String[0];
        }

        List<String> list = new ArrayList<String>(Arrays.asList(value));
        setField(instance, field, list);
    }

    void setCommand(Instance instance, String[] cmd) {
        if (cmd == null) {
            cmd = new String[0];
        }

        List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(cmd));
        setField(instance, FIELD_COMMAND, args);
    }

    void setEnvironment(Instance instance, String[] env) {
        if (env == null) {
            env = new String[0];
        }

        Map<String, String> envMap = new HashMap<String, String>();
        for (String e : env) {
            String[] kvp = e.split("=", 2);
            if (kvp.length == 2) {
                envMap.put(kvp[0], kvp[1]);
            } else if (kvp.length == 1) {
                // TODO Change the Rancher API to support valueless environment variables.
                // -e FOO and -e FOO="" are not the same thing.
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
        } else if (fieldValue instanceof List && fieldValue == null) {
            return new ArrayList();
        } else if (fieldValue instanceof Map && fieldValue == null) {
            return new HashMap();
        }
        return fieldValue;
    }

    private boolean isEmptyValue(Object fieldValue) {
        return fieldValue == null ||
                (fieldValue instanceof String && StringUtils.isEmpty((String) fieldValue)) ||
                (fieldValue instanceof Number && ((Number) fieldValue).longValue() == 0L) ||
                (fieldValue instanceof List && fieldValue == null) ||
                (fieldValue instanceof Map && fieldValue == null);
    }

    InspectContainerResponse transformInspect(Map<String, Object> inspect) {
        return jsonMapper.convertValue(inspect, InspectContainerResponse.class);
    }
}
