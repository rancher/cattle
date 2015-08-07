package io.cattle.platform.host.stats.utils;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicStringProperty;

public class StatsConstants {

    public static final String LINK_STATS = "stats";
    public static final String CONTAINER_STATS = "containerStats";
    public static final String HOST_STATS = "hostStats";

    public static final DynamicStringProperty CONTAINER_STATS_PATH = ArchaiusUtil.getString("link.containerstats.path");
    public static final DynamicStringProperty HOST_STATS_PATH = ArchaiusUtil.getString("link.hoststats.path");

}