package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.HashMap;
import java.util.Map;

@Type(list = false)
public class ComposeConfig {
    Map<String, String> templates = new HashMap<>();

    public ComposeConfig(String compose) {
        this.templates.put("compose.yml", compose);
    }

    public ComposeConfig() {
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }
}
