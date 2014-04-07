package io.cattle.platform.resource.pool.impl;

import static io.cattle.platform.core.model.tables.ResourcePoolTable.*;
import io.cattle.platform.core.model.ResourcePool;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.PooledResourceItemGeneratorFactory;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.util.type.CollectionUtils;

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
    public PooledResource allocateResource(Object pool, Object owner) {
        Map<Object,Object> keys = CollectionUtils.asMap(
                (Object)RESOURCE_POOL.POOL_TYPE, getResourceType(pool),
                (Object)RESOURCE_POOL.POOL_ID, getResourceId(pool),
                RESOURCE_POOL.OWNER_TYPE, getResourceType(owner),
                RESOURCE_POOL.OWNER_ID, getResourceId(owner));

        List<ResourcePool> resourcePool = objectManager.find(ResourcePool.class, keys);

        if ( resourcePool.size() > 0 ) {
            return new DefaultPooledResource(resourcePool.get(0).getItem());
        }

        String item = getItem(keys, pool);
        return item == null ? null : new DefaultPooledResource(item);
    }

    protected String getItem(Map<Object,Object> keys, Object pool) {
        PooledResourceItemGenerator generator = null;

        for ( PooledResourceItemGeneratorFactory factory : factories ) {
            generator = factory.getGenerator(pool);

            if ( generator != null ) {
                break;
            }
        }

        if ( generator == null ) {
            log.error("Failed to find generator for pool [{}]", pool);
            return null;
        }

        while ( generator.hasNext() ) {
            String item = generator.next();
            Map<Object,Object> newKeys = new HashMap<Object, Object>(keys);
            newKeys.put(RESOURCE_POOL.ITEM, item);

            Map<String,Object> props = objectManager.convertToPropertiesFor(ResourcePool.class, newKeys);
            try {
                return objectManager.create(ResourcePool.class, props).getItem();
            } catch ( DataAccessException e ) {
                log.debug("Failed to create item [{}]", item);
            }
        }

        return null;
    }

    protected String getResourceType(Object obj) {
        String type = objectManager.getType(obj);

        if ( type == null ) {
            throw new IllegalStateException("Failed to find resource type for [" + obj + "]");
        }

        return type;
    }

    protected long getResourceId(Object obj) {
        Object id = ObjectUtils.getId(obj);

        if ( id instanceof Number ) {
            return ((Number)id).longValue();
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
