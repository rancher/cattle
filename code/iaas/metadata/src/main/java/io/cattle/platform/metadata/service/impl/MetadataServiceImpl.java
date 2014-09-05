package io.cattle.platform.metadata.service.impl;

import static io.cattle.platform.util.type.CollectionUtils.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Offering;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.Zone;
import io.cattle.platform.core.util.HostnameGenerator;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.metadata.dao.MetadataDao;
import io.cattle.platform.metadata.data.MetadataEntry;
import io.cattle.platform.metadata.data.MetadataRedirectData;
import io.cattle.platform.metadata.service.MetadataService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class MetadataServiceImpl implements MetadataService {

//    private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

    ObjectManager objectManager;
    MetadataDao metadataDao;
    JsonMapper jsonMapper;

    @Override
    public boolean isAttachMetadata(Instance instance) {
        for ( NetworkService service : metadataDao.getMetadataServices(instance) ) {
            if ( DataAccessor.fieldBool(service, NetworkServiceConstants.FIELD_CONFIG_DRIVE) ) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Map<String,Object> getMetadata(Instance agentInstance, IdFormatter idFormatter) {
        return getMetaData(idFormatter, metadataDao.getMetadata(agentInstance));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String,Object> getMetadataForInstance(Instance instance, IdFormatter idFormatter) {
        MetadataEntry entry = metadataDao.getMetadataForInstance(instance);

        if ( entry == null ) {
            return null;
        }

        Map<String,Object> data = getMetaData(idFormatter, Arrays.asList(entry));
        if ( data.size() == 0 ) {
            return null;
        }

        Object value = data.values().iterator().next();
        if ( value instanceof Map<?, ?> ) {
            return (Map<String,Object>)value;
        }

        return null;
    }

    protected Map<String,Object> getMetaData(IdFormatter idFormatter, List<MetadataEntry> entries) {
        Map<Long,String> primaryIps = new HashMap<Long, String>();
        Map<Long,Map<String,Object>> instanceMetadatas = new HashMap<Long, Map<String,Object>>();
        Map<Long,String> userData = new HashMap<Long, String>();

        for ( MetadataEntry entry : entries ) {
            Instance instance = entry.getInstance();
            Nic nic = entry.getNic();
            IpAddress localIp = entry.getLocalIp();
            IpAddress publicIp = entry.getPublicIp();
            Credential credential = entry.getCredential();
            Network network = entry.getNetwork();
            Subnet subnet = entry.getSubnet();
            Volume volume = entry.getVolume();

            Map<String,Object> instanceMetadata = instanceMetadatas.get(instance.getId());

            if ( instanceMetadata == null ) {
                instanceMetadata = populateInstance(idFormatter, instance, network);
                instanceMetadatas.put(instance.getId(), instanceMetadata);
            }


            populateCredential(instanceMetadata, credential);
            populateNic(idFormatter, instanceMetadata, nic, network);
            populateIps(idFormatter, primaryIps, instanceMetadata, instance, nic, network, subnet, localIp, publicIp);
            populateVolume(instanceMetadata, instance, volume);

            userData.put(instance.getId(), instance.getUserdata());
        }

        Map<String,Object> metadata = new HashMap<String, Object>();

        for ( Map.Entry<Long, Map<String,Object>> entry : instanceMetadatas.entrySet() ) {
            Map<String,Object> fullData = new HashMap<String, Object>();

            fullData.put("meta-data", entry.getValue());
            fullData.put("user-data", userData.get(entry.getKey()));

            String key = primaryIps.get(entry.getKey());
            if ( key != null ) {
                metadata.put(key, fullData);
            }
        }

        return metadata;
    }

    protected void populateVolume(Map<String, Object> instanceMetadata, Instance instance, Volume volume) {
        if ( volume == null ) {
            return;
        }

        Integer deviceNumber = volume.getDeviceNumber();

        if ( deviceNumber == null ) {
            return;
        }

        char index = (char)('a' + deviceNumber.intValue());

        if ( deviceNumber == 0 ) {
            set(instanceMetadata, String.format("/dev/sd%s", index), "block-device-mapping", "ami");
            set(instanceMetadata, String.format("/dev/sd%s1", index), "block-device-mapping", "root");
        } else {
            set(instanceMetadata, "sd" + index, "block-device-mapping", "ebs" + (deviceNumber+1));
        }

        //TODO do something about swap and ephemeral
    }

    protected void populateIps(IdFormatter idFormatter, Map<Long,String> primaryIps, Map<String, Object> instanceMetadata, Instance instance, Nic nic, Network network, Subnet subnet,
            IpAddress localIp, IpAddress publicIp) {

        boolean firstNic = ( org.apache.commons.lang3.ObjectUtils.equals(nic.getDeviceNumber(), 0) );
        boolean primaryIp = localIp == null ? firstNic  : IpAddressConstants.ROLE_PRIMARY.equals(localIp.getRole());

        String localIpAddress = localIp == null ? null : localIp.getAddress();
        String addressKey = localIpAddress == null ? nic.getMacAddress() : localIpAddress;

        String localHostname = HostnameGenerator.lookup(true, instance, addressKey, network);
        String publicHostname = publicIp == null ? null : HostnameGenerator.lookup(false, instance, publicIp.getAddress(), network);

        if ( firstNic && primaryIp ) {
            instanceMetadata.put("hostname", localHostname);
            String existingKey = primaryIps.get(instance.getId());
            if ( existingKey == null || existingKey.contains(":") ) { // It's a MAC address
                primaryIps.put(instance.getId(), addressKey);
            }
        }

        @SuppressWarnings("unchecked")
        Map<String,Object> nicData = (Map<String, Object>)CollectionUtils.get(instanceMetadata, "network",
                "interfaces", "macs", nic.getMacAddress());

        if ( nicData == null ) {
            return;
        }


        setIfTrueOrNull(primaryIp, nicData, "local-hostname", localHostname);
        if ( localIpAddress != null ) {
            append(nicData, "local-ipv4s", localIpAddress);
        }

        if ( firstNic ) {
            setIfTrueOrNull(primaryIp, instanceMetadata, "local-hostname", localHostname);
            setIfTrueOrNull(primaryIp, instanceMetadata, "local-ipv4", localIpAddress);
        }

        if ( publicIp != null ) {
            if ( localIpAddress != null ) {
                set(nicData, localIpAddress, "ipv4-associations", publicIp.getAddress());
            }

            if ( publicHostname != null ) {
                append(nicData, "public-hostname", publicHostname);
                append(nicData, "public-ipv4s", publicIp.getAddress());

                if ( firstNic ) {
                    setIfTrueOrNull(primaryIp, instanceMetadata, "public-hostname", publicHostname);
                    setIfTrueOrNull(primaryIp, instanceMetadata, "public-ipv4", publicIp.getAddress());
                }
            }
        }

        if ( subnet != null ) {
            nicData.put("subnet-id", formatId(idFormatter, subnet));
            nicData.put("subnet-ipv4-cidr-block",
                    String.format("%s/%d", subnet.getNetworkAddress(), subnet.getCidrSize()));
            if ( nicData.containsKey("vpc-id") && nicData.get("vpc-ipv4-cidr-block") == null ) {
                nicData.put("vpc-ipv4-cidr-block",
                        String.format("%s/%d", subnet.getNetworkAddress(), subnet.getCidrSize()));
            }
        }

        //TODO don't have a security group concept yet
        nicData.put("security-group-ids", "");
        nicData.put("security-groups", "");
    }

    protected void setIfTrueOrNull(boolean condition, Map<String,Object> map, String key, String value) {
        if ( value == null ) {
            return;
        }

        if ( condition ) {
            map.put(key, value);
        } else {
            Object existing = map.get(key);
            if ( existing == null ) {
                map.put(key, value);
            }
        }
    }

    protected void append(Map<String,Object> map, String key, String data) {
        if ( data == null ) {
            return;
        }

        Object existing = map.get(key);
        if ( existing == null ) {
            map.put(key, data);
        } else {
            map.put(key, String.format("%s\n%s", existing.toString(), data));
        }
    }

    protected void populateCredential(Map<String, Object> instanceMetadata, Credential credential) {
        if ( credential == null ) {
            return;
        }

        int count = 0;

        @SuppressWarnings("unchecked")
        Map<String,Object> creds = (Map<String, Object>)get(instanceMetadata, "public-keys");

        if ( creds != null ) {
            count = creds.size();
        }

        String name = credential.getName();

        if ( name == null ) {
            name = "sshkey" + count;
        }

        set(instanceMetadata, credential.getPublicValue(), "public-keys", count + "=" + name, "openssh-key");
    }

    protected Map<String,Object> populateInstance(IdFormatter idFormatter, Instance instance, Network network) {
        Map<String,Object> data = new HashMap<String, Object>();

        data.put("ami-id", formatId(idFormatter, Image.class, instance.getImageId()));
        data.put("ami-manifest-path", "(unknown)");
        data.put("instance-action", "none");
        data.put("instance-id", "i-" + formatId(idFormatter, instance));
        data.put("instance-type", getInstanceType(instance));
        data.put("kernel-id", formatId(idFormatter, instance));
        set(data, getZone(idFormatter, instance), "placement", "availability-zone");

        //TODO don't really know what values are valid here
        data.put("profile", "default-paravirtual");

        //TODO don't have a concept of reservations or launch index yet
        data.put("ami-launch-index", "0");
        data.put("reservation-id", formatId(idFormatter, instance));

        return data;
    }

    protected void populateNic(IdFormatter idFormatter, Map<String,Object> instanceData, Nic nic, Network network) {
        Integer deviceNumber = nic.getDeviceNumber();
        String mac = nic.getMacAddress();
        if ( mac == null ) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String,Object> data = (Map<String, Object>)CollectionUtils.get(instanceData, "network", "interfaces", "macs", mac);

        if ( data == null ) {
            data = new HashMap<String, Object>();
            set(instanceData, data, "network", "interfaces", "macs", mac);
        } else {
            return;
        }

        data.put("device-number", deviceNumber);
        data.put("mac", nic.getMacAddress());
        data.put("owner-id", formatId(idFormatter, Instance.class, nic.getInstanceId()));

        //TODO don't have a security group concept yet
        data.put("security-group-ids", "");
        data.put("security-groups", "");

        data.put("vpc-id", formatId(idFormatter, Network.class, network.getId()));
        data.put("vpc-ipv4-cidr-block", DataAccessor.fieldString(network, NetworkConstants.FIELD_CIDR));

        if ( deviceNumber != null && deviceNumber.intValue() == 0 ) {
            instanceData.put("mac", nic.getMacAddress());
            set(instanceData, HostnameGenerator.getServicesDomain(network), "services", "domain");
        }
    }


    protected String getZone(IdFormatter idFormatter, Instance instance) {
        Zone zone = metadataDao.getZone(instance);

        if ( zone == null ) {
            return "unknown";
        }

        String name = zone.getName();

        return name == null ? formatId(idFormatter, zone) : name;
    }

    protected String getInstanceType(Instance instance) {
        Offering offering = metadataDao.getInstanceOffering(instance);
        String name = offering == null ? null : offering.getName();

        if ( name != null ) {
            return name;
        }

        Long mem = instance.getMemoryMb();
        if ( mem == null ) {
            mem = 64L;
        }
        Long cpu = DataAccessor.fieldLong(instance, InstanceConstants.FIELD_VCPU);
        if ( cpu == null ) {
            cpu = 1L;
        }

        StringBuilder buffer = new StringBuilder();
        if ( mem < 1024 ) {
            buffer.append(mem).append("mb-");
        } else {
            buffer.append(mem.floatValue()/1024).append("gb-");
        }

        buffer.append(cpu).append("cpu");

        return buffer.toString();
    }

    protected String formatId(IdFormatter idFormatter, Class<?> clz, Long id) {
        if ( id == null ) {
            return null;
        }

        String type = objectManager.getType(clz);
        return idFormatter.formatId(type, id).toString();
    }

    protected String formatId(IdFormatter idFormatter, Object obj) {
        if ( obj == null ) {
            return null;
        }

        String type = objectManager.getType(obj);
        return idFormatter.formatId(type, ObjectUtils.getId(obj)).toString();
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<MetadataRedirectData> getMetadataRedirects(Agent agent) {
        return metadataDao.getMetadataRedirects(agent);
    }

    public MetadataDao getMetadataDao() {
        return metadataDao;
    }

    @Inject
    public void setMetadataDao(MetadataDao metadataDao) {
        this.metadataDao = metadataDao;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }
}
