package io.cattle.platform.compose.export;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

public interface ComposeExportService {
    String buildComposeConfig(String stackId) throws IOException;

    Entry<String, String> buildLegacyComposeConfig(String stackId) throws IOException;
}
