package io.cattle.platform.docker.transform;

import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.docker.constants.DockerInstanceConstants.*;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class DockerTransformerImpl implements DockerTransformer {

    private static final String IMAGE_PREFIX = "docker:";
    private static final String IMAGE_KIND_PATTERN = "^(sim|docker):.*";

    @Inject
    JsonMapper jsonMapper;

    @Override
    public void transform(Map<String, Object> fromInspect, Instance instance) {
        InspectContainerResponse inspect = transformInspect(fromInspect);
        ContainerConfig containerConfig = inspect.getConfig();
        HostConfig hostConfig = inspect.getHostConfig();

        instance.setHostname(containerConfig.getHostName());
        instance.setExternalId(inspect.getId());
        setName(instance, inspect);
        setField(instance, FIELD_DOMAIN_NAME, containerConfig.getDomainName());
        setField(instance, FIELD_USER, containerConfig.getUser());
        setField(instance, FIELD_MEMORY, containerConfig.getMemoryLimit());
        setField(instance, FIELD_MEMORY_SWAP, containerConfig.getMemorySwap());
        setField(instance, FIELD_CPU_SHARES, containerConfig.getCpuShares());
        setField(instance, FIELD_CPU_SET, containerConfig.getCpuset());
        setField(instance, FIELD_TTY, containerConfig.isTty());
        setField(instance, FIELD_STDIN_OPEN, containerConfig.isStdinOpen());
        setImage(instance, containerConfig.getImage());
        setField(instance, FIELD_DIRECTORY, containerConfig.getWorkingDir());
        setEnvironment(instance, containerConfig.getEnv());
        setCommand(instance, containerConfig.getCmd());
        setListField(instance, FIELD_ENTRY_POINT, containerConfig.getEntrypoint());
        setVolumes(instance, containerConfig.getVolumes(), hostConfig.getBinds());
        setPorts(instance, safeGetExposedPorts(containerConfig), hostConfig.getPortBindings());
        setField(instance, FIELD_PRIVILEGED, hostConfig.isPrivileged());
        setField(instance, FIELD_PUBLISH_ALL_PORTS, hostConfig.isPublishAllPorts());
        setLxcConf(instance, hostConfig.getLxcConf());
        setListField(instance, FIELD_DNS, hostConfig.getDns());
        setListField(instance, FIELD_DNS_SEARCH, hostConfig.getDnsSearch());
        setCapField(instance, FIELD_CAP_ADD, hostConfig.getCapAdd());
        setCapField(instance, FIELD_CAP_DROP, hostConfig.getCapDrop());
        setRestartPolicy(instance, hostConfig.getRestartPolicy());
        setDevices(instance, hostConfig.getDevices());

        // TODO Network
        // TODO VolumesFrom
        // TODO Links
        // TODO SecurityOpt
        // TODO ExtraHosts
        // TODO Consider: AttachStdin AttachStdout AttachStderr StdinOnce
        // NetworkDisabled
    }

    private void setImage(Instance instance, String image) {
        // Somewhat of a hack, but needed for testing against sim contexts
        if ( !image.matches(IMAGE_KIND_PATTERN) ) {
            image = IMAGE_PREFIX + image;
        }

        setField(instance, FIELD_IMAGE_UUID, image);
    }

    void setName(Instance instance, InspectContainerResponse inspect) {
        String name = (String)inspect.getName();
        name = name.replaceFirst("/", "");
        instance.setName(name);
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
        if ( devices == null || devices.length == 0 ) {
            return;
        }

        List<String> instanceDevices = new ArrayList<String>();
        for ( Device d : devices ) {
            StringBuilder fullDevice = new StringBuilder(d.getPathOnHost()).append(":").append(d.getPathInContainer())
                    .append(":");
            if ( StringUtils.isEmpty(d.getcGroupPermissions()) ) {
                fullDevice.append("rwm");
            } else {
                fullDevice.append(d.getcGroupPermissions());
            }
            instanceDevices.add(fullDevice.toString());
        }

        setField(instance, FIELD_DEVICES, instanceDevices);
    }

    void setRestartPolicy(Instance instance, RestartPolicy restartPolicy) {
        if ( restartPolicy == null || StringUtils.isEmpty(restartPolicy.getName()) ) {
            return;
        }

        io.cattle.platform.core.addon.RestartPolicy rp = new io.cattle.platform.core.addon.RestartPolicy();
        rp.setMaximumRetryCount(restartPolicy.getMaximumRetryCount());
        rp.setName(restartPolicy.getName());
        setField(instance, FIELD_RESTART_POLICY, rp);
    }

    void setCapField(Instance instance, String field, Capability[] caps) {
        if ( caps == null || caps.length == 0 ) {
            return;
        }

        List<String> list = new ArrayList<String>();
        for ( Capability cap : caps ) {
            list.add(cap.toString());
        }
        setField(instance, field, list);
    }

    void setLxcConf(Instance instance, LxcConf[] lxcConf) {
        if ( lxcConf == null || lxcConf.length == 0 ) {
            return;
        }

        Map<String, String> instanceLxcConf = new HashMap<String, String>();
        for ( LxcConf lxc : lxcConf ) {
            instanceLxcConf.put(lxc.getKey(), lxc.getValue());
        }

        if ( instanceLxcConf.size() > 0 ) {
            setField(instance, FIELD_LXC_CONF, instanceLxcConf);
        }
    }

    void setPorts(Instance instance, ExposedPort[] exposedPorts, Ports portBindings) {
        if ( exposedPorts == null || exposedPorts.length == 0 ) {
            return;
        }

        List<String> ports = new ArrayList<String>();
        for ( ExposedPort ep : exposedPorts ) {
            String port = ep.toString();

            Binding[] bindings = portBindings == null || portBindings.getBindings() == null ? null : portBindings
                    .getBindings().get(ep);
            if ( bindings != null && bindings.length > 0 ) {
                for ( Binding b : bindings ) {
                    if ( b.getHostPort() != null ) {
                        String fullPort = b.getHostPort() + ":" + port;
                        ports.add(fullPort);
                    }
                }
            } else {
                ports.add(port);
            }
        }

        if ( ports.size() > 0 ) {
            setField(instance, FIELD_PORTS, ports);
        }
    }

    void setVolumes(Instance instance, Map<String, ?> volumes, String[] binds) {
        List<String> dataVolumes = new ArrayList<String>();
        if ( volumes != null && !volumes.isEmpty() ) {
            dataVolumes.addAll(volumes.keySet());
        }

        if ( binds != null && binds.length > 0 ) {
            dataVolumes.addAll(Arrays.asList(binds));
        }

        if ( dataVolumes.size() > 0 ) {
            setField(instance, FIELD_DATA_VOLUMES, dataVolumes);
        }
    }

    void setListField(Instance instance, String field, String[] value) {
        if ( value == null || value.length == 0 )
            return;

        List<String> list = new ArrayList<String>(Arrays.asList(value));
        setField(instance, field, list);
    }

    void setCommand(Instance instance, String[] cmd) {
        if ( cmd == null || cmd.length == 0 )
            return;

        setField(instance, FIELD_COMMAND, cmd[0]);

        if ( cmd.length > 1 ) {
            List<String> args = new ArrayList<String>(Arrays.asList(cmd));
            args.remove(0);
            setField(instance, FIELD_COMMAND_ARGS, args);
        }
    }

    void setEnvironment(Instance instance, String[] env) {
        if ( env == null || env.length == 0 )
            return;

        Map<String, String> envMap = new HashMap<String, String>();
        for ( String e : env ) {
            String[] kvp = e.split("=");
            envMap.put(kvp[0], kvp[1]);
        }
        setField(instance, FIELD_ENVIRONMENT, envMap);
    }

    private void setField(Instance instance, String field, Object fieldValue) {
        DataAccessor.fields(instance).withKey(field).set(fieldValue);
    }

    InspectContainerResponse transformInspect(Map<String, Object> inspect) {
        return jsonMapper.convertValue(inspect, InspectContainerResponse.class);
    }
}
