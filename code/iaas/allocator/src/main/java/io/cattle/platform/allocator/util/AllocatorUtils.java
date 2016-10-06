package io.cattle.platform.allocator.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AllocatorUtils {
    public static final Set<String> UNMANGED_STORAGE_POOLS = new HashSet<String>(Arrays.asList(new String[]{"docker", "sim"}));
}
