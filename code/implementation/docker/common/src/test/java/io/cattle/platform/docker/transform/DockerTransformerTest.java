package io.cattle.platform.docker.transform;

import static org.junit.Assert.*;
import static io.cattle.platform.docker.constants.DockerInstanceConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.json.JacksonJsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Links;
import com.github.dockerjava.api.model.LxcConf;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.VolumesFrom;

public class DockerTransformerTest {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testTransform() {
        DockerTransformerImpl transformer = transformer();

        Map<String, Method> configAccessors = new HashMap<String, Method>();
        Map<String, Method> hostConfigAccessors = new HashMap<String, Method>();
        Map<String, Object> inspectInput = inspect(configAccessors, hostConfigAccessors);
        Map<String, Object> configInput = ((Map<String, Object>)inspectInput.get("Config"));
        Map<String, Object> hostConfigInput = ((Map<String, Object>)inspectInput.get("HostConfig"));

        Instance instance = new InstanceRecord();
        transformer.transform(inspectInput, instance);
        assertEquals(configInput.get("Hostname"), instance.getHostname());
        assertEquals(((String)inspectInput.get("Name")).substring(1), instance.getName());
        assertTrue(StringUtils.isNotEmpty(instance.getName()));
        assertEquals(inspectInput.get("Id"), instance.getExternalId());
        assertTrue(StringUtils.isNotEmpty(instance.getExternalId()));
        assertEquals(configInput.get("Domainname"), DataAccessor.fields(instance).withKey(FIELD_DOMAIN_NAME).get());
        assertEquals(configInput.get("User"), DataAccessor.fields(instance).withKey(FIELD_USER).get());
        assertEquals(configInput.get("Memory"), DataAccessor.fields(instance).withKey(FIELD_MEMORY).get());
        assertEquals(configInput.get("MemorySwap"), DataAccessor.fields(instance).withKey(FIELD_MEMORY_SWAP).get());
        assertEquals(configInput.get("CpuShares"), DataAccessor.fields(instance).withKey(FIELD_CPU_SHARES).get());
        assertEquals(configInput.get("Tty"), DataAccessor.fields(instance).withKey(FIELD_TTY).get());
        assertEquals(configInput.get("OpenStdin"), DataAccessor.fields(instance).withKey(FIELD_TTY).get());
        assertEquals("docker:" + configInput.get("Image"), DataAccessor.fields(instance).withKey(FIELD_IMAGE_UUID)
                .get());
        assertEquals(configInput.get("WorkingDir"), DataAccessor.fields(instance).withKey(FIELD_DIRECTORY).get());

        String[] inputEnvs = (String[])configInput.get("Env");
        Map<String, String> instanceEnvs = (Map<String, String>)DataAccessor.fields(instance)
                .withKey(FIELD_ENVIRONMENT).get();
        assertEquals(inputEnvs.length, instanceEnvs.size());
        for ( String e : inputEnvs ) {
            String[] kvp = e.split("=");
            assertEquals(instanceEnvs.get(kvp[0]), kvp[1]);
        }

        // TODO test 0 COMMAND, 0 COMMAND ARGS
        String[] inputCmd = (String[])configInput.get("Cmd");
        String instanceCmd = (String)DataAccessor.fields(instance).withKey(FIELD_COMMAND).get();
        List<String> instanceCmdArgs = (List<String>)DataAccessor.fields(instance).withKey(FIELD_COMMAND_ARGS).get();
        assertEquals(inputCmd[0], instanceCmd);
        for ( int i = 1; i < inputCmd.length; i++ ) {
            assertEquals(inputCmd[i], instanceCmdArgs.get(i - 1));
        }

        String[] inputEP = (String[])configInput.get("Entrypoint");
        List<String> instanceEP = (List<String>)DataAccessor.fields(instance).withKey(FIELD_ENTRY_POINT).get();
        assertTrue(Arrays.equals(inputEP, instanceEP.toArray(new String[instanceEP.size()])));

        Map<String, Object> inputeVols = (Map<String, Object>)configInput.get("Volumes");
        String[] inputBinds = (String[])hostConfigInput.get("Binds");
        List<String> dataVolumes = (List<String>)DataAccessor.fields(instance).withKey(FIELD_DATA_VOLUMES).get();
        List<String> combinedVolumeInput = new ArrayList<String>();
        combinedVolumeInput.addAll(Arrays.asList(inputBinds));
        combinedVolumeInput.addAll(inputeVols.keySet());
        assertEquals(combinedVolumeInput.size(), dataVolumes.size());
        assertTrue(dataVolumes.containsAll(combinedVolumeInput));

        // Too hard to be clever about the ports
        List<String> actualPorts = (List<String>)DataAccessor.fields(instance).withKey(FIELD_PORTS).get();
        List<String> expectedPorts = new ArrayList<String>();
        expectedPorts.add("8090/udp");
        expectedPorts.add("12345:8080/tcp");
        expectedPorts.add("2222:8070/udp");
        expectedPorts.add("3333:8070/udp");
        assertEquals(expectedPorts.size(), actualPorts.size());
        assertTrue(actualPorts.containsAll(expectedPorts));

        assertEquals(hostConfigInput.get("Privileged"), DataAccessor.fields(instance).withKey(FIELD_PRIVILEGED).get());
        assertEquals(hostConfigInput.get("PublishAllPorts"),
                DataAccessor.fields(instance).withKey(FIELD_PUBLISH_ALL_PORTS).get());

        Map[] inputLxcConfs = (Map[])hostConfigInput.get("LxcConf");
        Map<String, String> instanceLxc = (Map<String, String>)DataAccessor.fields(instance).withKey(FIELD_LXC_CONF)
                .get();
        assertEquals(inputLxcConfs.length, instanceLxc.size());
        assertTrue(instanceLxc.size() > 0);
        for ( Map inputLxc : inputLxcConfs ) {
            assertNotNull(inputLxc.get("Key"));
            assertNotNull(inputLxc.get("Value"));
            String instanceLxcValue = instanceLxc.get(inputLxc.get("Key"));
            assertEquals(instanceLxcValue, inputLxc.get("Value"));
        }

        String[] inputDns = (String[])hostConfigInput.get("Dns");
        List<String> instanceDns = (List<String>)DataAccessor.fields(instance).withKey(FIELD_DNS).get();
        assertTrue(instanceDns.size() > 0);
        assertTrue(Arrays.equals(inputDns, instanceDns.toArray(new String[instanceDns.size()])));

        String[] inputDnsSearch = (String[])hostConfigInput.get("DnsSearch");
        List<String> instanceDnsSearch = (List<String>)DataAccessor.fields(instance).withKey(FIELD_DNS_SEARCH).get();
        assertTrue(instanceDnsSearch.size() > 0);
        assertTrue(Arrays.equals(inputDnsSearch, instanceDnsSearch.toArray(new String[instanceDnsSearch.size()])));

        String[] inputCapAdd = (String[])hostConfigInput.get("CapAdd");
        List<String> instanceCapAdd = (List<String>)DataAccessor.fields(instance).withKey(FIELD_CAP_ADD).get();
        assertTrue(instanceCapAdd.size() > 0);
        assertTrue(Arrays.equals(inputCapAdd, instanceCapAdd.toArray(new String[instanceCapAdd.size()])));

        String[] inputCapDrop = (String[])hostConfigInput.get("CapDrop");
        List<String> instanceCapDrop = (List<String>)DataAccessor.fields(instance).withKey(FIELD_CAP_DROP).get();
        assertTrue(instanceCapDrop.size() > 0);
        assertTrue(Arrays.equals(inputCapDrop, instanceCapDrop.toArray(new String[instanceCapDrop.size()])));

        Map<String, Object> inputRestartPolicy = (Map<String, Object>)hostConfigInput.get("RestartPolicy");
        io.cattle.platform.core.addon.RestartPolicy instanceRP = (io.cattle.platform.core.addon.RestartPolicy)DataAccessor
                .fields(instance).withKey(FIELD_RESTART_POLICY).get();
        assertEquals(inputRestartPolicy.get("MaximumRetryCount"), instanceRP.getMaximumRetryCount());
        assertEquals(inputRestartPolicy.get("Name"), instanceRP.getName());

        List<Map> inputDevices = (List<Map>)hostConfigInput.get("Devices");
        List<String> instanceDevices = (List<String>)DataAccessor.fields(instance).withKey(FIELD_DEVICES).get();
        assertEquals(inputDevices.size(), instanceDevices.size());
        assertTrue(instanceDevices.size() > 0);
        assertTrue(instanceDevices.contains("/dev/h/deviceName1:/dev/c/deviceName1:rw"));
        assertTrue(instanceDevices.contains("/dev/h/deviceName2:/dev/c/deviceName2:rwm"));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testTransformInternal() {
        DockerTransformerImpl transformer = transformer();

        Map<String, Method> configAccessors = new HashMap<String, Method>();
        Map<String, Method> hostConfigAccessors = new HashMap<String, Method>();
        Map<String, Object> inspectInput = inspect(configAccessors, hostConfigAccessors);

        InspectContainerResponse cc = transformer.transformInspect(inspectInput);
        try {
            Map<String, Object> configInput = ((Map<String, Object>)inspectInput.get("Config"));
            ContainerConfig configOutput = cc.getConfig();
            for ( Map.Entry<String, Method> configAccessor : configAccessors.entrySet() ) {
                Object expected = configInput.get(configAccessor.getKey());
                Object actual = configAccessor.getValue().invoke(configOutput);
                // System.out.println("Config: " + configAccessor.getKey() +
                // ": " + expected + " - " + actual);

                if ( configAccessor.getKey().equals("ExposedPorts") ) {
                    // Special case
                    Map<String, Object> expectedPorts = (Map<String, Object>)expected;
                    ExposedPort[] actualPorts = (ExposedPort[])actual;
                    assertEquals(expectedPorts.size(), actualPorts.length);
                    for ( ExposedPort port : actualPorts ) {
                        assertTrue(expectedPorts.containsKey(port.toString()));
                    }
                } else if ( expected instanceof String[] ) {
                    assertTrue(Arrays.equals((String[])expected, (String[])actual));
                } else {
                    assertEquals(expected, actual);
                }
            }

            Map<String, Object> hostConfigInput = ((Map<String, Object>)inspectInput.get("HostConfig"));
            HostConfig hostConfigOutput = cc.getHostConfig();
            for ( Map.Entry<String, Method> configAccessor : hostConfigAccessors.entrySet() ) {
                Object expected = hostConfigInput.get(configAccessor.getKey());
                Object actual = configAccessor.getValue().invoke(hostConfigOutput);
                // System.out.println("Host Config: " + configAccessor.getKey()
                // + ": " + expected + " - " + actual);

                if ( configAccessor.getKey().equals("Links") ) {
                    // Special case
                    String[] expectedLinks = (String[])expected;
                    Links actualLinks = (Links)actual;
                    assertEquals(expectedLinks.length, actualLinks.getLinks().length);
                    for ( Link link : actualLinks.getLinks() ) {
                        boolean found = false;
                        for ( String expectedLink : expectedLinks ) {
                            if ( expectedLink.equals(link.toString()) ) {
                                found = true;
                                break;
                            }
                        }
                        assertTrue(found);
                    }
                } else if ( configAccessor.getKey().equals("LxcConf") ) {
                    // Special case
                    Map[] expectedLxcConf = (Map[])expected;
                    LxcConf[] actualLxcConfs = (LxcConf[])actual;
                    for ( LxcConf lxcConf : actualLxcConfs ) {
                        boolean found = false;
                        for ( Map expectedLxc : expectedLxcConf ) {
                            if ( lxcConf.getKey().equals(expectedLxc.get("Key"))
                                    && lxcConf.getValue().equals(expectedLxc.get("Value")) ) {
                                found = true;
                                break;
                            }
                        }
                        assertTrue(found);
                    }
                } else if ( configAccessor.getKey().equals("PortBindings") ) {
                    Ports actualPortBindings = (Ports)actual;
                    Map<String, List<Map>> expectedPortBindings = (Map<String, List<Map>>)expected;
                    for ( Entry<ExposedPort, Binding[]> binding : actualPortBindings.getBindings().entrySet() ) {
                        List<Map> expectedValue = expectedPortBindings.get(binding.getKey().toString());
                        if ( expectedValue != null ) {
                            int index = 0;
                            for ( Binding b : binding.getValue() ) {
                                if ( !b.getHostIp().equals(expectedValue.get(index).get("HostIp"))
                                        || !b.getHostPort().equals(
                                                Integer.valueOf((String)expectedValue.get(index).get("HostPort"))) ) {
                                    fail();
                                }
                                index++;
                            }
                        }
                    }
                } else if ( configAccessor.getKey().equals("VolumesFrom") ) {
                    VolumesFrom[] actualVolumesFrom = (VolumesFrom[])actual;
                    String[] expectedVolumesFrom = (String[])expected;
                    for ( VolumesFrom v1 : actualVolumesFrom ) {
                        boolean found = false;
                        for ( String v2 : expectedVolumesFrom ) {
                            if ( v2.equals(v1.toString()) ) {
                                found = true;
                                break;
                            }
                        }
                        assertTrue(found);
                    }
                } else if ( configAccessor.getKey().equals("CapAdd") || configAccessor.getKey().equals("CapDrop") ) {
                    Capability[] actualCaps = (Capability[])actual;
                    String[] expectedCaps = (String[])expected;
                    for ( Capability c1 : actualCaps ) {
                        boolean found = false;
                        for ( String c2 : expectedCaps ) {
                            if ( c2.equals(c1.toString()) ) {
                                found = true;
                                break;
                            }
                        }
                        assertTrue(found);
                    }
                } else if ( configAccessor.getKey().equals("RestartPolicy") ) {
                    RestartPolicy actualRP = (RestartPolicy)actual;
                    Map<String, String> expectedRP = (Map<String, String>)expected;
                    assertEquals(actualRP.getMaximumRetryCount(), expectedRP.get("MaximumRetryCount"));
                    assertEquals(actualRP.getName(), expectedRP.get("Name"));
                } else if ( configAccessor.getKey().equals("Devices") ) {
                    Device[] actualDevices = (Device[])actual;
                    List<Map> expectedDevices = (List<Map>)expected;
                    int index = 0;
                    for ( Device d1 : actualDevices ) {
                        Map d2 = expectedDevices.get(index);
                        assertEquals(d2.get("PathInContainer"), d1.getPathInContainer());
                        assertEquals(d2.get("PathOnHost"), d1.getPathOnHost());
                        String perms = StringUtils.isEmpty((String)d2.get("CgroupPermissions")) ? "" : (String)d2
                                .get("CgroupPermissions");
                        assertEquals(perms, d1.getcGroupPermissions());
                        index++;
                    }
                } else if ( expected instanceof String[] ) {
                    assertTrue(Arrays.equals((String[])expected, (String[])actual));
                } else {
                    assertEquals(expected, actual);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void putConfigField(Map<String, Method> configFields, Map<String, Object> config, Class clazz, String field,
            Object value, String methodName) throws NoSuchMethodException {
        config.put(field, value);
        configFields.put(field, clazz.getDeclaredMethod(methodName, new Class[0]));
    }

    @SuppressWarnings({ "serial", "rawtypes", "unchecked" })
    Map<String, Object> inspect(Map<String, Method> configFields, Map<String, Method> hostConfigFields) {
        Map<String, Object> inspect = new HashMap<String, Object>();
        Map<String, Object> config = new HashMap<String, Object>();
        Map<String, Object> hostConfig = new HashMap<String, Object>();
        inspect.put("Config", config);
        inspect.put("HostConfig", hostConfig);
        inspect.put("Id", "docker-id");
        inspect.put("Name", "/docker_name");
        Class<ContainerConfig> configClazz = ContainerConfig.class;
        Class<HostConfig> hostConfigClazz = HostConfig.class;

        try {
            putConfigField(configFields, config, configClazz, "Hostname", "host name", "getHostName");
            putConfigField(configFields, config, configClazz, "Domainname", "domain name", "getDomainName");
            putConfigField(configFields, config, configClazz, "User", "user", "getUser");
            putConfigField(configFields, config, configClazz, "Memory", Long.valueOf(1), "getMemoryLimit");
            putConfigField(configFields, config, configClazz, "MemorySwap", Long.valueOf(2), "getMemorySwap");
            putConfigField(configFields, config, configClazz, "CpuShares", Integer.valueOf(3), "getCpuShares");
            putConfigField(configFields, config, configClazz, "Cpuset", "1,2", "getCpuset");
            putConfigField(configFields, config, configClazz, "AttachStdin", Boolean.TRUE, "isAttachStdin");
            putConfigField(configFields, config, configClazz, "AttachStdout", Boolean.TRUE, "isAttachStdout");
            putConfigField(configFields, config, configClazz, "AttachStderr", Boolean.TRUE, "isAttachStderr");
            putConfigField(configFields, config, configClazz, "Tty", Boolean.TRUE, "isTty");
            putConfigField(configFields, config, configClazz, "OpenStdin", Boolean.TRUE, "isStdinOpen");
            putConfigField(configFields, config, configClazz, "StdinOnce", Boolean.TRUE, "isStdInOnce");
            putConfigField(configFields, config, configClazz, "StdinOnce", Boolean.TRUE, "isStdInOnce");
            putConfigField(configFields, config, configClazz, "Image", "image", "getImage");
            putConfigField(configFields, config, configClazz, "WorkingDir", "working dir", "getWorkingDir");
            putConfigField(configFields, config, configClazz, "NetworkDisabled", Boolean.TRUE, "isNetworkDisabled");
            putConfigField(configFields, config, configClazz, "Env", new String[] { "env1=true", "env2=next" },
                    "getEnv");
            putConfigField(configFields, config, configClazz, "Cmd", new String[] { "sleep", "1" }, "getCmd");
            // TODO SecurityOpts not supported in java-docker (or Rancher)
            // putConfigField(configFields, config, configClazz, "SecurityOpts",
            // null, "getSecurityOpts");
            putConfigField(configFields, config, configClazz, "Entrypoint", new String[] { "entrypoint", "2" },
                    "getEntrypoint");
            putConfigField(configFields, config, configClazz, "Volumes", new HashMap<String, Object>() {
                {
                    put("/vol1", new HashMap());
                }
            }, "getVolumes");
            putConfigField(configFields, config, configClazz, "ExposedPorts", new HashMap<String, Object>() {
                {
                    put("8080/tcp", new HashMap());
                    put("8090/udp", new HashMap());
                    put("8070/udp", new HashMap());
                }
            }, "getExposedPorts");

            // HostConfig
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "Privileged", Boolean.TRUE, "isPrivileged");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "Binds", new String[] { "/tmp:/bar",
                    "/var:/baz:ro" }, "getBinds");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "Links", new String[] { "a:b", "c:d" },
                    "getLinks");

            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "PublishAllPorts", Boolean.TRUE,
                    "isPublishAllPorts");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "LxcConf", new Map[] { new HashMap() {
                {
                    put("Key", "lxc.cgroup.cpuset.cpus");
                    put("Value", "1,2");
                }
            } }, "getLxcConf");
            Map<String, List<Map>> portBindings = new HashMap<String, List<Map>>();
            portBindings.put("8080/tcp", Arrays.asList(new Map[] { new HashMap<String, String>() {
                {
                    put("HostPort", "12345");
                    put("HostIp", "1.0.2.2");
                }
            } }));
            portBindings.put("8070/udp", Arrays.asList(new Map[] { new HashMap<String, String>() {
                {
                    put("HostPort", "2222");
                    put("HostIp", "1.1.1.1");
                }
            }, new HashMap<String, String>() {
                {
                    put("HostPort", "3333");
                    put("HostIp", "0.0.0.0");
                }
            } }));
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "PortBindings", portBindings,
                    "getPortBindings");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "Dns", new String[] { "1.1.1.1" }, "getDns");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "DnsSearch",
                    new String[] { "a search domain" }, "getDnsSearch");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "ExtraHosts",
                    new String[] { "host1:1.2.3.4" }, "getExtraHosts");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "VolumesFrom", new String[] {
                    "containerA:rw", "containerB:ro" }, "getVolumesFrom");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "CapAdd",
                    new String[] { "NET_ADMIN", "MKNOD" }, "getCapAdd");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "CapDrop", new String[] { "NET_ADMIN",
                    "MKNOD" }, "getCapDrop");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "RestartPolicy",
                    new HashMap<String, Object>() {
                        {
                            put("Name", "on-failure");
                            put("MaximumRetryCount", 2);
                        }
                    }, "getRestartPolicy");
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "NetworkMode", "host", "getNetworkMode");
            List<Map> devices = new ArrayList<Map>();
            devices.add(new HashMap<String, String>() {
                {
                    put("PathOnHost", "/dev/h/deviceName1");
                    put("PathInContainer", "/dev/c/deviceName1");
                    put("CgroupPermissions", "rw");
                }
            });
            devices.add(new HashMap<String, String>() {
                {
                    put("PathOnHost", "/dev/h/deviceName2");
                    put("PathInContainer", "/dev/c/deviceName2");
                }
            });
            putConfigField(hostConfigFields, hostConfig, hostConfigClazz, "Devices", devices, "getDevices");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return inspect;
    }

    DockerTransformerImpl transformer() {
        DockerTransformerImpl transformer = new DockerTransformerImpl();
        transformer.jsonMapper = new JacksonJsonMapper();
        return transformer;
    }
}
