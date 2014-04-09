package io.cattle.platform.docker.process.instancehostmap;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.NicDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerIpAddressConstants;
import io.cattle.platform.docker.process.dao.DockerComputeDao;
import io.cattle.platform.docker.process.util.DockerProcessUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

import com.netflix.config.DynamicBooleanProperty;

public class DockerPostInstanceHostMapActivate extends AbstractObjectProcessLogic implements ProcessPostListener {

    public static final DynamicBooleanProperty DYNAMIC_ADD_IP = ArchaiusUtil.getBoolean("docker.compute.auto.add.host.ip");

    JsonMapper jsonMapper;
    IpAddressDao ipAddressDao;
    DockerComputeDao dockerDao;
    NicDao nicDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap)state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, map.getInstanceId());
        Host host = getObjectManager().loadResource(Host.class, map.getHostId());

        String dockerIp = DockerProcessUtils.getDockerIp(instance);
        Nic nic = nicDao.getPrimaryNic(instance);

        String hostIp = DataAccessor.fields(instance)
                .withKey(DockerInstanceConstants.FIELD_DOCKER_HOST_IP)
                .as(String.class);

        Map ports = DataAccessor.fields(instance)
                .withKey(DockerInstanceConstants.FIELD_DOCKER_PORTS)
                .as(jsonMapper, Map.class);

        if ( dockerIp != null ) {
            processDockerIp(instance, nic, dockerIp);
        }

        if ( hostIp != null && ports != null ) {
            processPorts(hostIp, ports, instance, nic, host);
        }

        return null;
    }

    protected void processDockerIp(Instance instance, Nic nic, String dockerIp) {
        if ( nic == null ) {
            return;
        }

        IpAddress dockerIpAddress = dockerDao.getDockerIp(dockerIp, instance);

        if ( dockerIpAddress == null ) {
            dockerIpAddress = ipAddressDao.mapNewIpAddress(nic,
                    IP_ADDRESS.SUBNET_ID, null,
                    IP_ADDRESS.KIND, DockerIpAddressConstants.KIND_DOCKER,
                    IP_ADDRESS.ADDRESS, dockerIp);
        }

        if ( dockerIpAddress.getKind().equals(DockerIpAddressConstants.KIND_DOCKER) ) {
            if ( ! dockerIp.equals(dockerIpAddress.getAddress()) ) {
                getObjectManager().setFields(dockerIpAddress,
                        IP_ADDRESS.ADDRESS, dockerIp);
            }
            createThenActivate(dockerIpAddress, null);
        }
    }

    protected void processPorts(String hostIp, Map<String,String> ports, Instance instance, Nic nic, Host host) {
        IpAddress ipAddress = getIpAddress(host, hostIp);
        IpAddress dockerIpAddress = dockerDao.getDockerIp(DockerProcessUtils.getDockerIp(instance), instance);
        Long privateIpAddressId = dockerIpAddress == null ? null : dockerIpAddress.getId();

        if ( ipAddress == null && DYNAMIC_ADD_IP.get() ) {
            ipAddress = ipAddressDao.assignNewAddress(host, hostIp);
        }

        if ( DYNAMIC_ADD_IP.get() ) {
            createThenActivate(ipAddress, null);
            for ( HostIpAddressMap map : getObjectManager().children(ipAddress, HostIpAddressMap.class) ) {
                if ( map.getHostId().longValue() == host.getId() ) {
                    createThenActivate(map, null);
                }
            }
        }

        Map<Integer,Port> existing = new HashMap<Integer, Port>();
        for ( Port port : getObjectManager().children(instance, Port.class) ) {
            existing.put(port.getPrivatePort(), port);
        }

        Long publicIpAddressId = ipAddress == null ? null : ipAddress.getId();

        for ( Map.Entry<String, String> entry : ports.entrySet() ) {
            PortSpec spec = new PortSpec(entry.getKey());
            Port port = existing.get(spec.getPrivatePort());

            if ( port == null ) {
                port = getObjectManager().create(Port.class,
                        PORT.ACCOUNT_ID, instance.getAccountId(),
                        PORT.INSTANCE_ID, instance.getId(),
                        PORT.PUBLIC_PORT, entry.getValue(),
                        PORT.PRIVATE_PORT, spec.getPrivatePort(),
                        PORT.PROTOCOL, spec.getProtocol(),
                        PORT.PUBLIC_IP_ADDRESS_ID, publicIpAddressId,
                        PORT.PRIVATE_IP_ADDRESS_ID, privateIpAddressId,
                        PORT.KIND, PortConstants.KIND_IMAGE);
            } else {
                if ( ! ObjectUtils.equals(port.getPublicPort(), entry.getValue()) ||
                        ! ObjectUtils.equals(port.getPrivateIpAddressId(), privateIpAddressId) ||
                        ! ObjectUtils.equals(port.getPublicIpAddressId(), publicIpAddressId) ) {
                    getObjectManager().setFields(port,
                            PORT.PUBLIC_PORT, entry.getValue(),
                            PORT.PRIVATE_IP_ADDRESS_ID, privateIpAddressId,
                            PORT.PUBLIC_IP_ADDRESS_ID, publicIpAddressId);
                }
            }
        }

        for ( Port port : getObjectManager().children(instance, Port.class) ) {
            createIgnoreCancel(port, null);
        }
    }

    protected IpAddress getIpAddress(Host host, String hostIp) {
        for ( IpAddress address : getObjectManager().mappedChildren(host, IpAddress.class) ) {
            if ( hostIp.equals(address.getAddress()) ) {
                return address;
            }
        }

        return null;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

    public DockerComputeDao getDockerDao() {
        return dockerDao;
    }

    @Inject
    public void setDockerDao(DockerComputeDao dockerDao) {
        this.dockerDao = dockerDao;
    }

    public NicDao getNicDao() {
        return nicDao;
    }

    @Inject
    public void setNicDao(NicDao nicDao) {
        this.nicDao = nicDao;
    }

}
