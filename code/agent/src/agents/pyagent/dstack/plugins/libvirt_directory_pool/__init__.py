from dstack.plugins.libvirt import enabled

if enabled():
    from directory_pool import DirectoryPoolDriver
    from dstack.plugins.libvirt import register_pool_driver

    register_pool_driver(DirectoryPoolDriver())
