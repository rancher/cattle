package io.github.ibuildthecloud.dstack.allocator.constraint;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationLog;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.dao.GenericMapDao;
import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.InstanceHostMap;

import java.util.List;

import javax.inject.Inject;

import com.netflix.config.DynamicLongProperty;

public class ComputeConstraintProvider implements AllocationConstraintsProvider {

    public static final DynamicLongProperty DEFAULT_COMPUTE = ArchaiusUtil.getLong("instance.compute.default");

    GenericMapDao mapDao;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        Instance instance = attempt.getInstance();

        if ( instance == null ) {
            return;
        }

        List<? extends InstanceHostMap> maps = mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId());

        if ( maps.size() > 0 ) {
            return;
        }

        long compute = instance.getCompute() == null ? DEFAULT_COMPUTE.get() : instance.getCompute();

        constraints.add(new ComputeContstraint(compute));
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}
