package io.cattle.platform.extension.dynamic.dao.impl;

import static io.cattle.platform.core.model.tables.ExternalHandlerExternalHandlerProcessMapTable.*;
import static io.cattle.platform.core.model.tables.ExternalHandlerProcessTable.*;
import static io.cattle.platform.core.model.tables.ExternalHandlerTable.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jooq.Field;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.cattle.platform.core.cache.DBCacheManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.extension.dynamic.dao.ExternalHandlerDao;
import io.cattle.platform.extension.dynamic.data.ExternalHandlerData;

public class ExternalHandlerDaoImpl extends AbstractJooqDao implements ExternalHandlerDao {

    LoadingCache<String, List<? extends ExternalHandlerData>> cache;

    @Inject
    DBCacheManager dbCacheManager;

    @PostConstruct
    public void init() {
        cache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<? extends ExternalHandlerData>>() {
                @Override
                public List<? extends ExternalHandlerData> load(String key) throws Exception {
                    return getExternalHandlerDataInternal(key);
                }
            });
        dbCacheManager.register(cache);
    }

    @Override
    public List<? extends ExternalHandlerData> getExternalHandlerData(String processName) {
        return cache.getUnchecked(processName);
    }

    protected List<? extends ExternalHandlerData> getExternalHandlerDataInternal(String processName) {
    	List<Field<?>> fields = new ArrayList<Field<?>>(Arrays.asList(EXTERNAL_HANDLER.fields()));
    	fields.add(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.ON_ERROR);
        fields.add(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EVENT_NAME);

        return create()
                .select(fields)
                .from(EXTERNAL_HANDLER)
                .join(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP)
                    .on(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_ID.eq(EXTERNAL_HANDLER.ID))
                .join(EXTERNAL_HANDLER_PROCESS)
                    .on(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.EXTERNAL_HANDLER_PROCESS_ID.eq(EXTERNAL_HANDLER_PROCESS.ID))
                .where(
                        EXTERNAL_HANDLER_PROCESS.NAME.eq(processName)
                        .and(EXTERNAL_HANDLER.STATE.eq(CommonStatesConstants.ACTIVE))
                        .and(EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP.STATE.eq(CommonStatesConstants.ACTIVE))
                        .and(EXTERNAL_HANDLER_PROCESS.STATE.eq(CommonStatesConstants.ACTIVE)))
                .fetchInto(ExternalHandlerData.class);
    }

}
