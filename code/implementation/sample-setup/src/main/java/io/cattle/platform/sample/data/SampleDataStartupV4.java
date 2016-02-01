package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.DynamicSchemaTable.*;
import static io.cattle.platform.core.model.tables.MachineDriverTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.core.model.tables.records.DynamicSchemaRecord;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV4 extends AbstractSampleData {

    @Override
    protected String getName() {
        return "sampleDataVersion4";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
    Account serviceAccount = createByUuid(Account.class, "service", ACCOUNT.KIND, "service", ACCOUNT.NAME, "service");
    toCreate.add(serviceAccount);
    Map<Object, Object> props = CollectionUtils.asMap
            ((Object) DYNAMIC_SCHEMA.NAME, "machine", DYNAMIC_SCHEMA.DEFINITION, " {\n" +
                            "    \"collectionMethods\": [ \"GET\", \"POST\", \"DELETE\" ],\n" +
                            "    \"resourceMethods\": [ \"GET\", \"DELETE\" ],\n" +
                            "    \"resourceFields\": {\n" +
                            "        \"name\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": false,\n" +
                            "            \"minLength\": 1,\n" +
                            "            \"required\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"authCertificateAuthority\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"authKey\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"labels\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineInstallUrl\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"dockerVersion\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineOpt\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineInsecureRegistry\": {\n" +
                            "            \"type\": \"array[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineRegistryMirror\": {\n" +
                            "            \"type\": \"array[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineLabel\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineStorageDriver\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineEnv\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n", DYNAMIC_SCHEMA.PARENT, "physicalHost", DYNAMIC_SCHEMA.ACCOUNT_ID, serviceAccount.getId(),
                    "roles", Arrays.asList("owner", "member", "project"),
                    DYNAMIC_SCHEMA.ACCOUNT_ID, null);
    Map<String, Object> properties = objectManager.convertToPropertiesFor(DynamicSchemaRecord.class, props);
    toCreate.add(getObjectManager().create(DynamicSchema.class, properties));
    props = CollectionUtils.asMap
            ((Object) DYNAMIC_SCHEMA.NAME, "machine", DYNAMIC_SCHEMA.DEFINITION, " {\n" +
                            "    \"collectionMethods\": [ \"GET\", \"POST\" ],\n" +
                            "    \"resourceMethods\": [ \"GET\", \"PUT\", \"DELETE\" ],\n" +
                            "    \"resourceFields\": {\n" +
                            "        \"name\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": false,\n" +
                            "            \"minLength\": 1,\n" +
                            "            \"required\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"authCertificateAuthority\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"authKey\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"extractedConfig\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"update\": true\n" +
                            "        },\n" +
                            "        \"labels\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineInstallUrl\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"dockerVersion\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineOpt\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineInsecureRegistry\": {\n" +
                            "            \"type\": \"array[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineRegistryMirror\": {\n" +
                            "            \"type\": \"array[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineLabel\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineStorageDriver\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        },\n" +
                            "        \"engineEnv\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true,\n" +
                            "            \"create\": true\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n", DYNAMIC_SCHEMA.PARENT, "physicalHost", DYNAMIC_SCHEMA.ACCOUNT_ID, serviceAccount.getId(),
                    "roles", Arrays.asList("service"),
                    DYNAMIC_SCHEMA.ACCOUNT_ID, null);
    properties = objectManager.convertToPropertiesFor(DynamicSchemaRecord.class, props);
    toCreate.add(getObjectManager().create(DynamicSchema.class, properties));
    props = CollectionUtils.asMap
            ((Object) DYNAMIC_SCHEMA.NAME, "machine", DYNAMIC_SCHEMA.DEFINITION, " {\n" +
                            "    \"collectionMethods\": [ \"GET\" ],\n" +
                            "    \"resourceMethods\": [ \"GET\" ],\n" +
                            "    \"resourceFields\": {\n" +
                            "        \"name\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": false,\n" +
                            "            \"minLength\": 1,\n" +
                            "            \"required\": true\n" +
                            "        },\n" +
                            "        \"authCertificateAuthority\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"authKey\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"driver\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"labels\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"engineInstallUrl\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"dockerVersion\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"engineOpt\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"engineInsecureRegistry\": {\n" +
                            "            \"type\": \"array[string]\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"engineRegistryMirror\": {\n" +
                            "            \"type\": \"array[string]\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"engineLabel\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"engineStorageDriver\": {\n" +
                            "            \"type\": \"string\",\n" +
                            "            \"nullable\": true\n" +
                            "        },\n" +
                            "        \"engineEnv\": {\n" +
                            "            \"type\": \"map[string]\",\n" +
                            "            \"nullable\": true\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n", DYNAMIC_SCHEMA.PARENT, "physicalHost", DYNAMIC_SCHEMA.ACCOUNT_ID, serviceAccount.getId(),
                    "roles", Arrays.asList("user", "admin", "readAdmin", "readonly"),
                    DYNAMIC_SCHEMA.ACCOUNT_ID, null);
    properties = objectManager.convertToPropertiesFor(DynamicSchemaRecord.class, props);
    toCreate.add(getObjectManager().create(DynamicSchema.class, properties));

    toCreate.add(createByUuid(MachineDriver.class, "packet", MACHINE_DRIVER.KIND, "machineDriver", MACHINE_DRIVER.MD5CHECKSUM,
            "558a264002166868acc9d175357c12c4", MACHINE_DRIVER.URI,
            "https://github.com/packethost/docker-machine-driver-packet/releases/" +
                    "download/v0.1.0/docker-machine-driver-packet_linux-amd64.zip", MACHINE_DRIVER.NAME, "packet",
            MACHINE_DRIVER.STATE, CommonStatesConstants.INACTIVE));
    toCreate.add(createByUuid(MachineDriver.class, "ubiquity", MACHINE_DRIVER.KIND, "machineDriver", MACHINE_DRIVER.MD5CHECKSUM,
            "7fba983dfdb040311a93d217b12161d1", MACHINE_DRIVER.URI,
            "https://github.com/ubiquityhosting/docker-machine-driver-ubiquity/releases/download/" +
                    "v0.0.2/docker-machine-driver-ubiquity_linux-amd64", MACHINE_DRIVER.NAME, "ubiquity",
            MACHINE_DRIVER.STATE, CommonStatesConstants.INACTIVE));
    }
}
