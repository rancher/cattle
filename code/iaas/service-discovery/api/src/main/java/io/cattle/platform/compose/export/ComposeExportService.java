package io.cattle.platform.compose.export;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;

import java.util.List;
import java.util.Map.Entry;

public interface ComposeExportService {

    Entry<String, String> buildComposeConfig(List<? extends Service> services, Stack stack, boolean combined);

    String buildDockerComposeConfig(List<? extends Service> services, Stack stack, boolean combined);

    String buildRancherComposeConfig(List<? extends Service> services, boolean combined);

}
