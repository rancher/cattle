from cattle import utils
import json


class QemuImg(object):

    @staticmethod
    def info(file, format=None):
        args = ['qemu-img', 'info', '--output', 'json']
        if format is not None:
            args.append('-f')
            args.append(format)

        args.append(file)

        return json.loads(utils.get_command_output(args))

    @staticmethod
    def create(file, cwd=None, format=None, backing_file=None, size=None):
        args = ['qemu-img', 'create']

        if format is not None:
            args.append('-f')
            args.append(format)

        if backing_file is not None:
            args.append('-o')
            args.append('backing_file={0}'.format(backing_file))

        args.append(file)

        if size is not None:
            args.append(size)

        utils.get_command_output(args, cwd=cwd)
