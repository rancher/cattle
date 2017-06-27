package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.StorageDriverDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lifecycle.SecretsLifecycleManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Date;
import java.util.List;

public class SecretsLifecycleManagerImpl implements SecretsLifecycleManager {

    TokenService tokenService;
    StorageDriverDao storageDriverDao;
    JsonMapper jsonMapper;

    public SecretsLifecycleManagerImpl(TokenService tokenService, StorageDriverDao storageDriverDao, JsonMapper jsonMapper) {
        this.tokenService = tokenService;
        this.storageDriverDao = storageDriverDao;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Object create(Instance instance) {
        return setSecrets(instance);
    }

    @Override
    public void persistCreate(Instance instance, Object obj) {
        if (obj == null) {
            return;
        }
        if (!(obj instanceof Volume)) {
            throw new IllegalStateException("Invalid secrets object passed to persist");
        }

        storageDriverDao.assignSecretsVolume(instance, (Volume) obj);
    }

    protected Volume setSecrets(Instance instance) {
        List<SecretReference> secrets = DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_SECRETS,
                SecretReference.class, jsonMapper);
        if (secrets == null || secrets.isEmpty()) {
            return null;
        }

        StorageDriver driver = storageDriverDao.findSecretsDriver(instance.getAccountId());
        if (driver == null) {
            return null;
        }

        String token = tokenService.generateToken(CollectionUtils.asMap("uuid", instance.getUuid()),
                new Date(System.currentTimeMillis() + 31556926000L));

        return storageDriverDao.createSecretsVolume(instance, driver, token);
    }

}
