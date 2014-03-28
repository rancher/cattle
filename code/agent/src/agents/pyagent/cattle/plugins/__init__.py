import sys
import os
import imp
import logging


log = logging.getLogger("agent")


def _load(module, plugin_path):
    std_name = "cattle.plugins.%s" % module
    if std_name in sys.modules:
        return

    log.info("Loading Plugin: %s from %s", module, plugin_path)
    m = imp.find_module(module, [plugin_path])
    return imp.load_module(std_name, m[0], m[1], m[2])


def _init(full_path):
    for d in os.listdir(full_path):
        plugin_path = os.path.join(full_path, d)
        if os.path.exists(os.path.join(plugin_path, "__init__.py")):
            _load(d, full_path)


def load():
    _init(os.path.dirname(os.path.abspath(__file__)))
