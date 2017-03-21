package io.cattle.platform.vm.process;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.platform.core.addon.VirtualMachineDisk;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class VirtualMachinePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    private static final String[] CAPS = new String[] { "NET_ADMIN" };
    private static final String[] VOLUMES = new String[] { "/var/lib/rancher/vm:/vm", "/var/run/rancher:/var/run/rancher" };
    private static final String[] DEVICES = new String[] { "/dev/kvm:/dev/kvm", "/dev/net/tun:/dev/net/tun" };
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_.-]+");

    @Inject
    VolumeDao volumeDao;

    @Inject
    StoragePoolDao storagePoolDao;

    @Inject
    ServiceDao serviceDao;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.KIND_VIRTUAL_MACHINE.equals(instance.getKind())) {
            return null;
        }

        Map<Object, Object> fields = new HashMap<>();
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);

        labels.put("io.rancher.scheduler.affinity:host_label_soft", "kvm=true");
        labels.put(SystemLabels.LABEL_RANCHER_NETWORK, "true");
        labels.put(SystemLabels.LABEL_VM, "true");
        long mem = 512L;
        if (instance.getMemoryMb() == null) {
            labels.put(SystemLabels.LABEL_VM_MEMORY, "512");
        } else {
            mem = instance.getMemoryMb();
            labels.put(SystemLabels.LABEL_VM_MEMORY, instance.getMemoryMb().toString());
        }
        labels.put(SystemLabels.LABEL_VM_USERDATA, instance.getUserdata());
        labels.put(SystemLabels.LABEL_VM_VCPU, DataAccessor.fieldString(instance, InstanceConstants.FIELD_VCPU));

        fields.put(InstanceConstants.FIELD_LABELS, labels);
        fields.put(ObjectMetaDataManager.CAPABILITIES_FIELD, Arrays.asList("console"));

        Map<String, Object> env = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_ENVIRONMENT);
        env.put("RANCHER_NETWORK", "true");

        List<String> dataVolumes = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DATA_VOLUMES);
        List<String> devices = DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_DEVICES);
        List<String> caps = DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_CAP_ADD);

        for (String volume : VOLUMES) {
            addToList(dataVolumes, volume);
        }
        for (String device : DEVICES) {
            addToList(devices, device);
        }
        for (String cap : CAPS) {
            addToList(caps, cap);
        }

        String volumeDriver = DataAccessor.fieldString(instance, InstanceConstants.FIELD_VOLUME_DRIVER);
        Object objectDisks = DataAccessor.field(instance, InstanceConstants.FIELD_DISKS, Object.class);
        if (objectDisks instanceof List<?>) {
            String namePrefix = instance.getName();
            Long svcIndexId = instance.getServiceIndexId();

            String uuidPart = null;
            if (svcIndexId != null) {
                Service svc = serviceDao.getServiceByServiceIndexId(svcIndexId);
                uuidPart = svc.getUuid().substring(0, 7);
            } else {
                uuidPart = instance.getUuid().substring(0, 7);
            }

            if (StringUtils.isBlank(namePrefix) || !NAME_PATTERN.matcher(namePrefix).matches()) {
                namePrefix = uuidPart;
            } else {
                namePrefix += "-" + uuidPart;
            }

            boolean rootFound = false;
            int index = 0;
            List<VirtualMachineDisk> disks = jsonMapper.convertCollectionValue(objectDisks, List.class, VirtualMachineDisk.class);
            for (int i = 0; i < disks.size(); i++) {
                VirtualMachineDisk disk = disks.get(i);
                if (disk.isRoot() && rootFound) {
                    continue;
                }

                String name = disk.getName();
                boolean assignedName = false;
                if (StringUtils.isBlank(name)) {
                    assignedName = true;
                    name = String.format("%s-%02d", namePrefix, index);
                }

                List<? extends Volume> volumes = volumeDao.findSharedOrUnmappedVolumes(instance.getAccountId(), name);
                if (volumes.size() == 0 && !assignedName) {
                    name = String.format("%s-%s", namePrefix, name);
                    volumes = volumeDao.findSharedOrUnmappedVolumes(instance.getAccountId(), name);
                }

                String localDriver = disk.getDriver();
                if (localDriver == null) {
                    localDriver = volumeDriver;
                }
                List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName(instance.getAccountId(), localDriver);
                String blockDevPath = null;
                if (pools.size() > 0) {
                    blockDevPath = DataAccessor.fieldString(pools.get(0), StoragePoolConstants.FIELD_BLOCK_DEVICE_PATH);
                }

                if (volumes.size() == 0) {
                    Map<String, String> opts = disk.getOpts();
                    if (opts == null) {
                        opts = new HashMap<>();
                    }

                    opts.put("vm", "true");

                    if (disk.getSize() != null) {
                        opts.put("size", disk.getSize());
                    }

                    if (disk.getReadIops() != null) {
                        opts.put("read-iops", disk.getReadIops().toString());
                    }

                    if (disk.getWriteIops() != null) {
                        opts.put("write-iops", disk.getWriteIops().toString());
                    }

                    if (StringUtils.isNotEmpty(blockDevPath)) {
                        opts.put("dont-format", "true");
                        if (disk.isRoot()) {
                            String image = StringUtils.removeStart(DataAccessor.fieldString(instance, InstanceConstants.FIELD_IMAGE_UUID), "docker:");
                            if (StringUtils.isNotBlank(image)) {
                                opts.put(VolumeConstants.DRIVER_OPT_BASE_IMAGE, image);
                            }
                            opts.remove("size");
                        }
                    }

                    objectManager.create(Volume.class,
                            VOLUME.NAME, name,
                            VOLUME.ACCOUNT_ID, instance.getAccountId(),
                            VOLUME.ACCESS_MODE, VolumeConstants.ACCESS_MODE_SINGLE_INSTANCE_RW,
                            VolumeConstants.FIELD_VOLUME_DRIVER, localDriver,
                            VolumeConstants.FIELD_VOLUME_DRIVER_OPTS, opts);
                } else {
                    /* Use name from DB because of case sensitivity */
                    Volume v = volumes.get(0);
                    name = v.getName();
                    if (!VolumeConstants.ACCESS_MODE_SINGLE_INSTANCE_RW.equals(v.getAccessMode())) {
                        objectManager.setFields(v, VOLUME.ACCESS_MODE, VolumeConstants.ACCESS_MODE_SINGLE_INSTANCE_RW);
                    }
                }

                String diskNameInContainer = String.format("disk%02d", index);
                String dataVolumeString = String.format("%s:/volumes/%s", name, diskNameInContainer);
                if (disk.isRoot()) {
                    rootFound = true;
                    dataVolumeString = String.format("%s:/image", name);
                    diskNameInContainer = "root";
                } else {
                    index++;
                }

                if (StringUtils.isNotEmpty(blockDevPath)) {
                    String deviceOnHost = Paths.get(blockDevPath, name).toString();
                    String deviceInContainer = String.format("/dev/vm/%s", diskNameInContainer);
                    String deviceParam = String.format("%s:%s", deviceOnHost, deviceInContainer);
                    addToList(devices, deviceParam);
                }

                addToList(dataVolumes, dataVolumeString);
            }
        }

        // TODO: I'd really like to remove this
        List<Object> command = new ArrayList<Object>(DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_COMMAND));
        if (!command.contains("-m")) {
            command.add("-m");
            command.add(labels.get(SystemLabels.LABEL_VM_MEMORY));
        }
        if (!command.contains("-smp")) {
            command.add("-smp");
            command.add("cpus=" + labels.get(SystemLabels.LABEL_VM_VCPU));
        }

        fields.put(DockerInstanceConstants.FIELD_COMMAND, command);
        fields.put(InstanceConstants.FIELD_ENVIRONMENT, env);
        fields.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
        fields.put(DockerInstanceConstants.FIELD_DEVICES, devices);
        fields.put(DockerInstanceConstants.FIELD_CAP_ADD, caps);
        fields.put(INSTANCE.MEMORY_MB, mem);

        return new HandlerResult(true, fields);
    }

    protected void addToList(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

}
