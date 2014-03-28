package io.cattle.platform.engine.process;

import io.cattle.platform.extension.ExtensionPoint;

public interface ExtensionBasedProcessDefinition {

    ExtensionPoint getPreProcessListenersExtensionPoint();

    ExtensionPoint getProcessHandlersExtensionPoint();

    ExtensionPoint getPostProcessListenersExtensionPoint();

}
