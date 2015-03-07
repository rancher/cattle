package io.cattle.platform.framework.event.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingData {

    Map<String, Boolean> options = new HashMap<String, Boolean>();
    List<Map<String, Object>> resources = new ArrayList<Map<String, Object>>();

    public Map<String, Boolean> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Boolean> options) {
        this.options = options;
    }

    public List<Map<String, Object>> getResources() {
        return resources;
    }

    public void setResources(List<Map<String, Object>> resources) {
        this.resources = resources;
    }

}
