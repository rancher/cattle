package io.cattle.platform.hazelcast.membership.dao;

import io.cattle.platform.core.model.ClusterMembership;
import io.cattle.platform.hazelcast.membership.ClusterConfig;

import java.io.IOException;
import java.util.List;

public interface ClusterMembershipDAO {

    void checkin(String uuid, ClusterConfig config, boolean initial) throws IOException;

    List<? extends ClusterMembership> listMembers();

    void delete(ClusterMembership member);

}
