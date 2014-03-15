import libvirt


class LibvirtConnection:
    def __init__(self):
        self.conn = None

    def __enter__(self):
        self.conn = self.open()
        return self.conn

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.conn.close()

    @staticmethod
    def open():
        return libvirt.open('qemu:///system')
