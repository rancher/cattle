package io.cattle.platform.compose.export;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;

import java.util.List;

public interface ComposeExportService {
    String buildComposeConfig(List<? extends Service> services, Stack stack);

    String buildDockerComposeConfig(List<? extends Service> services, Stack stack);
}
