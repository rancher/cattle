package io.cattle.platform.configitem.exception;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;

import java.util.ArrayList;
import java.util.List;

public class ConfigTimeoutException extends TimeoutException {

    private static final long serialVersionUID = -6028393678833505990L;

    ConfigUpdateRequest request;
    List<ConfigUpdateItem> items;

    public ConfigTimeoutException(ConfigUpdateRequest request, List<ConfigUpdateItem> items) {
        super(message(request, items));
        this.request = request;
        this.items = items;
    }

    public ConfigTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigTimeoutException(String message) {
        super(message);
    }

    public ConfigTimeoutException(Throwable cause) {
        super(cause);
    }

    private static String message(ConfigUpdateRequest request, List<ConfigUpdateItem> items) {
        List<String> names = new ArrayList<String>();

        for (ConfigUpdateItem item : items) {
            names.add(item.getName());
        }

        return String.format("Timeout waiting for [%s] to update %s", request.getClient(), names);
    }

    public ConfigUpdateRequest getRequest() {
        return request;
    }

    public List<ConfigUpdateItem> getItems() {
        return items;
    }

}
