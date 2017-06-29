package io.cattle.platform.framework.event;

import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.data.PingData;

import java.util.Map;

public class Ping extends EventVO<PingData> {

    public static final String STATS = "stats";
    public static final String RESOURCES = "resources";
    public static final String INSTANCES = "instances";

    public Ping() {
        setName(FrameworkEvents.PING);
        setData(new PingData());
    }

    public void setOption(String name, boolean value) {
        getData().getOptions().put(name, value);
    }

    public Ping withOption(String name, boolean value) {
        setOption(name, value);
        return this;
    }

    public boolean getOption(String name) {
        PingData data = getData();
        if (data == null) {
            return false;
        }

        Map<String, Boolean> options = data.getOptions();
        Boolean value = options.get(name);
        return value == null ? false : value;
    }

}