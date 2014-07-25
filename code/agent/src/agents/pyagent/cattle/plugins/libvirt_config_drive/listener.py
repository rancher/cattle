import glob
import logging
import os
import shutil

from hashlib import sha1
from cattle.type_manager import get_type, MARSHALLER
from cattle.plugins.libvirt.config import LibvirtConfig
from cattle.utils import temp_file_in_work_dir, get_command_output

log = logging.getLogger('libvirt-cloud-config')


class ConfigDriveComputeListener(object):
    def before_start(self, instance, host, progress, config):
        try:
            metadata = instance.data.metadata['meta-data']
            userdata = instance.data.metadata['user-data']
        except (KeyError, AttributeError):
            return

        log.info('Setting up config drive for %s', instance.uuid)

        os_metadata = self._create_os_meta_data(instance, metadata)
        if userdata is None:
            userdata = ''

        self._filter_meta_data(metadata)

        marshaller = get_type(MARSHALLER)
        meta_data_content = marshaller.to_string(metadata)
        os_meta_data_content = marshaller.to_string(os_metadata)

        content = [
            ('ec2/2009-04-04/meta-data.json', meta_data_content),
            ('ec2/2009-04-04/user-data', userdata),
            ('ec2/latest/meta-data.json', meta_data_content),
            ('ec2/latest/user-data', userdata,),
            ('openstack/2012-08-10/meta_data.json', os_meta_data_content),
            ('openstack/2012-08-10/user_data', userdata,),
            ('openstack/latest/meta_data.json', os_meta_data_content),
            ('openstack/latest/user_data', userdata)
        ]

        hash = self._hash_content(content)
        iso_file = self._write_iso(instance, content, hash)

        config.set_param('config-drive-iso', iso_file)

    def _write_iso(self, instance, content, hash):
        file_name = 'config-{0}-{1}.iso'.format(instance.uuid, hash)
        output_dir = LibvirtConfig.config_drive_directory()
        output_file = os.path.join(output_dir, file_name)

        if os.path.exists(output_file):
            log.info('Using existing config drive for %s', instance.uuid)
            return output_file

        temp_dir = temp_file_in_work_dir(output_dir)
        temp_iso = os.path.join(temp_dir, 'output.iso')
        content_dir = os.path.join(temp_dir, 'content')

        for item in content:
            output_content_file = os.path.join(content_dir, item[0])
            output_content_dir = os.path.dirname(output_content_file)

            if not os.path.exists(output_content_dir):
                os.makedirs(output_content_dir)

            with open(output_content_file, 'w') as f:
                f.write(item[1])

        output = get_command_output([
            LibvirtConfig.genisoimage(),
            '-allow-lowercase',
            '-allow-multidot',
            '-J',
            '-l',
            '-ldots',
            '-o', temp_iso,
            '-publisher', 'Cattle',
            '-quiet',
            '-r',
            '-V', 'config-2',
            content_dir
        ])

        log.debug(output)

        os.rename(temp_iso, output_file)
        log.info('Created %s for instance %s', output_file, instance.uuid)

        shutil.rmtree(temp_dir)

        return output_file

    def _hash_content(self, content):
        digest = sha1()

        for item in content:
            digest.update(item[0])
            if item[1] is not None:
                digest.update(item[1])

        return digest.hexdigest()

    def _filter_meta_data(self, metadata):
        try:
            new_keys = {}
            for name, v in metadata['public-keys'].items():
                if '=' in name:
                    name = name.split('=', 1)[0]

                new_keys[name] = v

            metadata['public-keys'] = new_keys
        except (KeyError, AttributeError):
            pass

    def _create_os_meta_data(self, instance, metadata):
        data = {
            'availability_zone': 'nova',
            'files': [],
            'meta': {},
            'public_keys': {},
            'uuid': instance.uuid
        }

        try:
            data['hostname'] = metadata['hostname']
            data['name'] = metadata['hostname'].split('.', 1)[0]
        except KeyError:
            pass

        try:
            for name, v in metadata['public-keys'].items():
                if '=' in name:
                    name = name.split('=', 1)[1]

                data['public_keys'][name] = v['openssh-key']
        except (KeyError, AttributeError):
            pass

        return data

    def before_stop(self, instance, host):
        pattern = os.path.join(LibvirtConfig.config_drive_directory(),
                               'config-{0}-*.iso'.format(instance.uuid))

        for iso in glob.glob(pattern):
            os.remove(iso)
