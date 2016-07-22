package io.cattle.platform.process.externalevent;

public class ExternalEventConstants {

    public static final String KIND_VOLUME_EVENT = "externalVolumeEvent";
    public static final String KIND_STORAGE_POOL_EVENT = "externalStoragePoolEvent";
    public static final String KIND_EXTERNAL_DNS_EVENT = "externalDnsEvent";
    public static final String KIND_SERVICE_EVENT = "externalServiceEvent";
    public static final String KIND_EXTERNAL_HOST_EVENT = "externalHostEvent";
    public static final String KIND_EXTERNAL_EVENT = "externalEvent";
    public static final String FIELD_REPORTED_ACCOUNT_ID = "reportedAccountId";
    public static final String FIELD_HOST_UUIDS = "hostUuids";
    public static final String TYPE_VOLUME_CREATE = "volume.create";
    public static final String TYPE_VOLUME_DELETE = "volume.delete";
    public static final String TYPE_SERVICE_CREATE = "service.create";
    public static final String TYPE_SERVICE_UPDATE = "service.update";
    public static final String TYPE_SERVICE_DELETE = "service.remove";
    public static final String TYPE_STACK_DELETE=  "stack.remove";
    public static final String TYPE_HOST_EVACUATE = "host.evacuate";
    public static final String VOLUME_POOL_LOCK_NAME = "VOLUME";
    public static final String STORAGE_POOL_LOCK_NAME = "STORAGEPOOL";
    public static final String EXERNAL_DNS_LOCK_NAME = "EXTERNALDNS";
    public static final String SERVICE_LOCK_NAME = "SERVICE";
    public static final String FIELD_STORAGE_POOL = "storagePool";
    public static final String FIELD_VOLUME = "volume";
    public static final String FIELD_SERVICE = "service";
    public static final String FIELD_ZONE_ID = "zoneId";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DRIVER_NAME = "driverName";
    public static final String FIELD_ATTACHED_STATE = "attachedState";
    public static final String FIELD_ALLOC_STATE = "allocationState";
    public static final String FIELD_DEV_NUM = "deviceNumber";
    public static final String FIELD_VOL_ID = "deviceNumber";
    public static final String FIELD_SP_ID = "deviceNumber";
    public static final String FIELD_ENVIRIONMENT = "environment";
    public static final String FIELD_ENVIRIONMENT_ID = "environmentId";
    public static final String FIELD_EXTERNAL_ID = "externalId";
    public static final String PROC_VOL_MAP_CREATE = "volumestoragepoolmap.create";
    public static final String PROC_VOL_MAP_ACTIVATE = "volumestoragepoolmap.activate";
    public static final String PROC_VOL_DEACTIVATE = "volume.deactivate";
    public static final String PROC_VOL_REMOVE = "volume.remove";
    public static final String FIELD_SERVICE_NAME = "serviceName";
    public static final String FIELD_STACK_NAME = "stackName";
    public static final String FIELD_FQDN = "fqdn";
    public static final String FIELD_HOST_LABEL = "hostLabel";
    public static final String FIELD_HOST_ID = "hostId";
    public static final String FIELD_DELETE_HOST = "deleteHost";

}
