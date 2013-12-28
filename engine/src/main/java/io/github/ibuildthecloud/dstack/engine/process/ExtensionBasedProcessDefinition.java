package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.extension.ExtensionPoint;

public interface ExtensionBasedProcessDefinition {

    ExtensionPoint getPreProcessListenersExtensionPoint();

    ExtensionPoint getProcessHandlersExtensionPoint();

    ExtensionPoint getPostProcessListenersExtensionPoint();

}
