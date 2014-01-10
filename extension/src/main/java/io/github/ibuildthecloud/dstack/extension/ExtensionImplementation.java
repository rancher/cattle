package io.github.ibuildthecloud.dstack.extension;

import java.util.Map;

public interface ExtensionImplementation {

    String getName();

    String getClassName();

    Map<String,String> getProperties();

}
