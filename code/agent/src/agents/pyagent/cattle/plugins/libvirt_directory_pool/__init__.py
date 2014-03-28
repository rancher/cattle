from cattle.plugins.libvirt import enabled

if enabled():
    from directory_pool import DirectoryPoolDriver
    from cattle.plugins.libvirt import register_pool_driver

    register_pool_driver(DirectoryPoolDriver())
