from cattle.plugins.libvirt import enabled

if enabled():
    from .qemu_img_volume import Qcow2ImageDriver
    from cattle.plugins.libvirt import register_volume_driver

    register_volume_driver(Qcow2ImageDriver())
