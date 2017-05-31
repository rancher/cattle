package io.cattle.platform.engine.server;

import org.apache.commons.lang3.tuple.Pair;

public interface Cluster {

    Pair<Integer, Integer> getCountAndIndex();

}
