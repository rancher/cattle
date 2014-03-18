import libvirt


class LibvirtConnection:
    def __init__(self, host):
        self.conn = None
        self.host = host

    def __enter__(self):
        self.conn = self.open(self.host)
        return self.conn

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.conn.close()

    @staticmethod
    def open(type):
        if type in ['qemu', 'kvm']:
            return libvirt.open('qemu:///system')
        elif type == 'xen':
            return libvirt.open('xen:///')
        else:
            # This assumes you have setup libvirt aliases
            #     http://libvirt.org/uri.html#URI_config
            return libvirt.open(type)
