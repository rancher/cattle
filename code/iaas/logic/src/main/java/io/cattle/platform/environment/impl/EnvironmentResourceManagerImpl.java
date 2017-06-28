package io.cattle.platform.environment.impl;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.metadata.service.MetadataObjectFactory;
import io.cattle.platform.metadata.service.impl.MetadataImpl;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class EnvironmentResourceManagerImpl implements EnvironmentResourceManager {

    private static final Class<?>[] LOAD_TYPES = new Class<?>[]{
        Host.class,
        Instance.class,
        Service.class,
        Stack.class,
        Network.class
    };

    MetadataObjectFactory factory = new MetadataObjectFactory();
    LoopManager loopManager;
    LockManager lockManager;
    ObjectManager objectManager;

    LoadingCache<Long, Metadata> metadataCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, Metadata>() {
                @Override
                public Metadata load(Long accountId) throws Exception {
                    return buildMetadata(accountId);
                }
            });


    private Metadata buildMetadata(long accountId) {
        MetadataImpl metadata = new MetadataImpl(accountId, factory, loopManager, lockManager);

        for (Class<?> clz : LOAD_TYPES) {
            for (Object obj : objectManager.find(clz,
                    ObjectMetaDataManager.ACCOUNT_FIELD, accountId,
                    ObjectMetaDataManager.REMOVED_FIELD, null)) {
                metadata.changed(obj);
            }
        }

        return metadata;
    }

    @Override
    public Metadata getMetadata(long accountId) {
        return metadataCache.getUnchecked(accountId);
    }

}
