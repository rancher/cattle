package io.cattle.platform.core.dao;

import java.util.concurrent.Callable;

public interface DataDao {

    String getOrCreate(String key, boolean visible, Callable<String> generator);

}
