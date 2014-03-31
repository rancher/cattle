Customizing Libvirt
===================

The libvirt code will by default look for a template named custom_template.tml and use it if it is found.  Copy this contents of the `cattle-home` folder to your $CATTLE_HOME directory and then restart Cattle.  If done properly you should have `${CATTLE_HOME}/etc/cattle/agents/pyagent/cattle/plugins/libvirt/custom_template.tmpl` and when the agent from the hypervisor connects you should see `/var/lib/cattle/pyagent/cattle/plugins/libvirt/custom_template.tmpl` on the hypervisor.

You can add whatever you want to the custom template, the example will setup a 9pfs share from /var/lib/guest-shared-fs and be available under the 9pfs tag shared-fs.  The share is by default read only.  Such a setup makes it really easy to develop something on the host system and then share the files with a VM.

After you start/restart a VM using this configuration, refer to the data.libvirt.xml property of the virtual machine to ensure that your changes ended up in the libvirt domain XML.
