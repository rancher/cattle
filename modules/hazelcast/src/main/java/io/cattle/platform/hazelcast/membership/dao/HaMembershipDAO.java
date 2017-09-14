package io.cattle.platform.hazelcast.membership.dao;

import io.cattle.platform.core.model.HaMembership;
import io.cattle.platform.hazelcast.membership.ClusterConfig;

import java.io.IOException;
import java.util.List;

public interface HaMembershipDAO {

    void checkin(String uuid, ClusterConfig config, boolean initial) throws IOException;

    List<? extends HaMembership> listMembers();

    void delete(HaMembership member);

}
