package org.apache.cloudstack.db.jooq.mapper;

import java.io.Serializable;

import javax.inject.Inject;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordType;
import org.jooq.impl.DefaultRecordMapper;

import com.cloud.utils.db.CloudStackJooqMapper;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GenericDaoBase;

public class CloudStackRecordMapperProvider implements RecordMapperProvider {

    @Inject
    EntityManager entityManager;

    @Override
    public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType, Class<? extends E> type) {
        @SuppressWarnings("unchecked")
        GenericDaoBase<E, Serializable> dao = (GenericDaoBase<E, Serializable>) GenericDaoBase.getDao(type);
        if ( dao == null ) {
            return new DefaultRecordMapper<R, E>(recordType, type);
        } else {
            return new CloudStackJooqMapper<R, E>(recordType, dao);
        }
    }

}
