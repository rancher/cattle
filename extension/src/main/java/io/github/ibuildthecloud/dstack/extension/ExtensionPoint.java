package io.github.ibuildthecloud.dstack.extension;

import java.util.List;

public interface ExtensionPoint {

    String getName();

    List<ExtensionImplementation> getImplementations();

    String getListSetting();

    String getExcludeSetting();

    String getIncludeSetting();

}
