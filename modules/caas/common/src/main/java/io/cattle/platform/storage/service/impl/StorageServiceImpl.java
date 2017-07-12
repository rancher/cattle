package io.cattle.platform.storage.service.impl;

import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.StorageDriverConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.exception.ExecutionErrorException;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.model.tables.StoragePoolTable.*;

public class StorageServiceImpl implements StorageService {

    public static final DynamicStringProperty DEFAULT_REGISTRY = ArchaiusUtil.getString("registry.default");
    public static final DynamicStringListProperty WHITELIST_REGISTRIES = ArchaiusUtil.getList("registry.whitelist");

    ObjectManager objectManager;
    GenericResourceDao genericResourceDao;
    LockManager lockManager;
    StoragePoolDao storagePoolDao;

    public StorageServiceImpl(ObjectManager objectManager, GenericResourceDao genericResourceDao, LockManager lockManager, StoragePoolDao storagePoolDao) {
        super();
        this.objectManager = objectManager;
        this.genericResourceDao = genericResourceDao;
        this.lockManager = lockManager;
        this.storagePoolDao = storagePoolDao;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void validateImageAndSetImage(Object inputObject, boolean required) throws ClientVisibleException {
        String image = DataAccessor.fromMap(inputObject).withKey(InstanceConstants.FIELD_IMAGE).as(String.class);
        String imageUuid = DataAccessor.fromMap(inputObject).withKey(InstanceConstants.FIELD_IMAGE_UUID).as(String.class);
        if (StringUtils.isBlank(image) && StringUtils.isBlank(imageUuid)) {
            if (required) {
                throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, InstanceConstants.FIELD_IMAGE);
            }
            return;
        }

        String finalImage = image;
        if (StringUtils.isBlank(image) && StringUtils.isNotBlank(imageUuid)) {
            finalImage = StringUtils.removeStart(imageUuid, "docker:");
        }

        validateImage(finalImage);
        DataAccessor.fromMap(inputObject).withKey(InstanceConstants.FIELD_IMAGE).set(finalImage);
        DataAccessor.fromMap(inputObject).withKey(InstanceConstants.FIELD_IMAGE_UUID).set("docker:" + finalImage);
    }

    private void validateImage(String image) throws ClientVisibleException {
        DockerImage dockerImage = DockerImage.parse(image);
        if (dockerImage == null) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                    "Failed to parse image", null);
        }

        String registry = dockerImage.getServer();
        List<String> whitelistRegistries = WHITELIST_REGISTRIES.get();
        String defaultRegistry = DEFAULT_REGISTRY.get();
        String userProvidedRegistry = dockerImage.getServer();

        if (!image.contains(registry)) {
            if(!StringUtils.isBlank(defaultRegistry)) {
                userProvidedRegistry = defaultRegistry;
            }
        }

        if (whitelistRegistries.size() > 0) {
            if (!StringUtils.isEmpty(userProvidedRegistry) && !StringUtils.isBlank(userProvidedRegistry)) {
                if (!whitelistRegistries.contains(userProvidedRegistry)) {
                    throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                            "The provided registry is not whitelisted", null);
                }
            }
            else {
                if (!StringUtils.isEmpty(defaultRegistry) && !StringUtils.isBlank(defaultRegistry)) {
                    if (!whitelistRegistries.contains(defaultRegistry)) {
                        throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                                "The default registry is not whitelisted", null);
                    }
                }
            }
        }
    }

    @Override
    public void setupPools(final StorageDriver storageDriver) {
        String scope = DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_SCOPE);
        if (StorageDriverConstants.SCOPE_CUSTOM.equals(scope)) {
            return;
        }

        lockManager.lock(new StorageDriverLock(storageDriver), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                setupPoolsWithLock(storageDriver);
            }
        });
    }

    @Override
    public String normalizeImageUuid(String image) {
        DockerImage dockerImage = DockerImage.parse(image);
        if (dockerImage == null) {
            return image;
        }

        String fullImageName = dockerImage.getFullName();
        String registry = dockerImage.getServer();
        if (!fullImageName.startsWith(registry)) {
            String defaultRegistry = DEFAULT_REGISTRY.get();
            if (image.contains(registry)) {
                fullImageName = registry + "/" + fullImageName;
            }
            else if (!StringUtils.isBlank(defaultRegistry) && !StringUtils.isEmpty(defaultRegistry)) {
                fullImageName = defaultRegistry + "/" + fullImageName;
            }
        }

        try {
            validateImage(fullImageName.toString());
        } catch (ClientVisibleException e) {
            throw new ExecutionErrorException("Image [" + fullImageName.toString() +
                    "] is no longer a valid image to run");
        }

        return fullImageName;
    }

    private void validateImageUuid(String image) {
        DockerImage dockerImage = DockerImage.parse(image);
        if (dockerImage == null) {
            return;
        }

        String registry = dockerImage.getServer();
        List<String> whitelistRegistries = WHITELIST_REGISTRIES.get();
        String defaultRegistry = DEFAULT_REGISTRY.get();
        String userProvidedRegistry = dockerImage.getServer();

        if (!image.contains(registry)) {
            if(!StringUtils.isBlank(defaultRegistry)) {
                userProvidedRegistry = defaultRegistry;
            }
        }


        if (whitelistRegistries.size() > 0) {
            if (!StringUtils.isEmpty(userProvidedRegistry) && !StringUtils.isBlank(userProvidedRegistry)) {
                if (!whitelistRegistries.contains(userProvidedRegistry)) {
                    throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                            "The provided registry is not whitelisted", null);
                }
            }
            else {
                if (!StringUtils.isEmpty(defaultRegistry) && !StringUtils.isBlank(defaultRegistry)) {
                    if (!whitelistRegistries.contains(defaultRegistry)) {
                        throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_OPTION,
                                "The default registry is not whitelisted", null);
                    }
                }
            }
        }
    }


    protected Map<String, Object> getStoragePoolProperties(StorageDriver storageDriver) {
        return objectManager.convertToPropertiesFor(StoragePool.class, CollectionUtils.asMap(STORAGE_POOL.ACCOUNT_ID, storageDriver.getAccountId(),
                STORAGE_POOL.NAME, storageDriver.getName(),
                STORAGE_POOL.DRIVER_NAME, storageDriver.getName(),
                STORAGE_POOL.STORAGE_DRIVER_ID, storageDriver.getId(),
                STORAGE_POOL.VOLUME_ACCESS_MODE,
                    DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_VOLUME_ACCESS_MODE),
                StoragePoolConstants.FIELD_VOLUME_CAPABILITIES,
                    DataAccessor.fieldString(storageDriver, StoragePoolConstants.FIELD_VOLUME_CAPABILITIES)));
    }

    protected void setupPoolsWithLock(StorageDriver storageDriver) {
        boolean localScope = StorageDriverConstants.SCOPE_LOCAL.equals(DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_SCOPE));
        StoragePool globalPool = objectManager.findAny(StoragePool.class,
                STORAGE_POOL.STORAGE_DRIVER_ID, storageDriver.getId(),
                STORAGE_POOL.REMOVED, null);
        Map<Long, Long> hostToPool = storagePoolDao.findStoragePoolHostsByDriver(storageDriver.getAccountId(),
                storageDriver.getId());
        for (Map.Entry<Long, Long> entry : hostToPool.entrySet()) {
            Long hostId = entry.getKey();
            Long storagePoolId = entry.getValue();
            if (storagePoolId != null) {
                continue;
            }

            if (localScope) {
                storagePoolDao.mapNewPool(hostId, getStoragePoolProperties(storageDriver));
            } else {
                if (globalPool == null) {
                    globalPool = storagePoolDao.mapNewPool(hostId, getStoragePoolProperties(storageDriver));
                } else {
                    storagePoolDao.mapPoolToHost(globalPool.getId(), hostId);
                }
            }
        }
    }
}