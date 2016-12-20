package io.cattle.platform.resource.pool.impl;

import static io.cattle.platform.core.model.tables.ResourcePoolTable.*;

import io.cattle.platform.core.model.ResourcePool;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.PooledResourceItemGeneratorFactory;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcePoolManagerImpl implements ResourcePoolManager {

    private static final Logger log = LoggerFactory.getLogger(ResourcePoolManagerImpl.class);

    ObjectManager objectManager;
    List<PooledResourceItemGeneratorFactory> factories;

    @Override
    public List<PooledResource> allocateResource(Object pool, Object owner, PooledResourceOptions options) {
        String qualifier = options.getQualifier();
        int count = options.getCount();

        String poolType = getResourceType(pool);
        long poolId = getResourceId(pool);
        String ownerType = getResourceType(owner);
        long ownerId = getResourceId(owner);

        Map<Object, Object> keys = CollectionUtils.asMap((Object) RESOURCE_POOL.POOL_TYPE, poolType,
                (Object) RESOURCE_POOL.POOL_ID, poolId,
                RESOURCE_POOL.QUALIFIER, qualifier,
                RESOURCE_POOL.OWNER_TYPE, ownerType,
                RESOURCE_POOL.OWNER_ID, ownerId);

        List<ResourcePool> resourcePools = new ArrayList<ResourcePool>(objectManager.find(ResourcePool.class, keys));
        List<PooledResource> result = new ArrayList<PooledResource>();

        for (ResourcePool resourcePool : resourcePools) {
            result.add(new DefaultPooledResource(resourcePool.getItem()));
        }

        while (result.size() < count) {
            String item = getItem(keys, pool, qualifier, options.getRequestedItem());

            if (item == null) {
                break;
            } else {
                log.info("Assigning [{}] from pool [{}:{}] to owner [{}:{}]", item, poolType, poolId, ownerType, ownerId);
            }

            result.add(new DefaultPooledResource(item));
        }

        if (result.size() != count) {
            log.info("Failed to find [{}] items for pool [{}:{}] and owner [{}:{}]", count, poolType, poolId, ownerType, ownerId);

            releaseResource(pool, owner, options);
            return null;
        }

        return result;
    }

    @Override
    public void releaseResource(Object pool, Object owner) {
        releaseResource(pool, owner, new PooledResourceOptions());
    }

    @Override
    public void releaseResource(Object pool, Object owner, PooledResourceOptions options) {
        String poolType = getResourceType(pool);
        long poolId = getResourceId(pool);
        String ownerType = getResourceType(owner);
        long ownerId = getResourceId(owner);

        Map<Object, Object> keys = CollectionUtils.asMap((Object) RESOURCE_POOL.POOL_TYPE, poolType, (Object) RESOURCE_POOL.POOL_ID, poolId,
                RESOURCE_POOL.QUALIFIER, options.getQualifier(), RESOURCE_POOL.OWNER_TYPE, ownerType, RESOURCE_POOL.OWNER_ID, ownerId);

        for (ResourcePool resource : objectManager.find(ResourcePool.class, keys)) {
            log.info("Releasing [{}] id [{}] to pool [{}:{}] from owner [{}:{}]", resource.getItem(), resource.getId(), poolType, poolId, ownerType, ownerId);
            objectManager.delete(resource);
        }
    }

    @Override
    public void transferResource(Object pool, Object owner, Object newOwner) {
        transferResource(pool, owner, newOwner, new PooledResourceOptions());
    }

    @Override
    public void transferResource(Object pool, Object owner, Object newOwner, PooledResourceOptions options) {
        String poolType = getResourceType(pool);
        long poolId = getResourceId(pool);
        String ownerType = getResourceType(owner);
        long ownerId = getResourceId(owner);
        String newOwnerType = getResourceType(newOwner);
        long newOwnerId = getResourceId(newOwner);

        Map<Object, Object> keys = CollectionUtils.asMap((Object) RESOURCE_POOL.POOL_TYPE, poolType,
                (Object) RESOURCE_POOL.POOL_ID, poolId,
                RESOURCE_POOL.QUALIFIER, options.getQualifier(), RESOURCE_POOL.OWNER_TYPE, ownerType,
                RESOURCE_POOL.OWNER_ID, ownerId);

        for (ResourcePool resource : objectManager.find(ResourcePool.class, keys)) {
            log.info("Transfering [{}] id [{}] from pool [{}:{}] from owner [{}:{}] to owner [{}:{}]",
                    resource.getItem(), resource.getId(), poolType,
                    poolId, ownerType, ownerId, newOwnerType, newOwnerId);
            resource.setOwnerType(newOwnerType);
            resource.setOwnerId(newOwnerId);
            objectManager.persist(resource);
        }

    }

    @Override
    public PooledResource allocateOneResource(Object pool, Object owner, PooledResourceOptions options) {
        List<PooledResource> resources = allocateResource(pool, owner, options);
        return (resources == null || resources.size() == 0) ? null : resources.get(0);
    }

    protected String getItem(Map<Object, Object> keys, Object pool, String qualifier, String tryItem) {
        PooledResourceItemGenerator generator = null;

        for (PooledResourceItemGeneratorFactory factory : factories) {
            generator = factory.getGenerator(pool, qualifier);

            if (generator != null) {
                break;
            }
        }

        if (generator == null) {
            log.error("Failed to find generator for pool [{}]", pool);
            return null;
        }

        while (generator.hasNext()) {
            String item = null;
            if (tryItem == null) {
                item = generator.next();
            } else {
                item = generator.isInPool(tryItem) ? tryItem : generator.next();
                tryItem = null;
            }
            Map<Object, Object> newKeys = new HashMap<Object, Object>(keys);
            newKeys.put(RESOURCE_POOL.ITEM, item);

            Map<String, Object> props = objectManager.convertToPropertiesFor(ResourcePool.class, newKeys);
            try {
                return objectManager.create(ResourcePool.class, props).getItem();
            } catch (DataAccessException e) {
                log.debug("Failed to create item [{}]", item);
            }
        }

        return null;
    }

    protected String getResourceType(Object obj) {
        if (GLOBAL.equals(obj)) {
            return GLOBAL;
        }

        String type = objectManager.getType(obj);

        if (type == null) {
            throw new IllegalStateException("Failed to find resource type for [" + obj + "]");
        }

        return type;
    }

    protected long getResourceId(Object obj) {
        if (GLOBAL.equals(obj)) {
            return 1;
        }

        Object id = ObjectUtils.getId(obj);

        if (id instanceof Number) {
            return ((Number) id).longValue();
        }

        throw new IllegalStateException("Failed to find resource id for [" + obj + "]");
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public List<PooledResourceItemGeneratorFactory> getFactories() {
        return factories;
    }

    @Inject
    public void setFactories(List<PooledResourceItemGeneratorFactory> factories) {
        this.factories = factories;
    }

}
