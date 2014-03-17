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
    def open(host):
        if host in ['qemu', 'kvm']:
            return libvirt.open('qemu:///system')
        elif host == 'xen':
            return libvirt.open('xen:///')

        raise Exception('Unsupported connection [{0}]'.format(host))
