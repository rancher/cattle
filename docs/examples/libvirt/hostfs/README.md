Customizing Libvirt
===================

The libvirt code will by default look for a template named `custom_template.tml` and use it if it is found.  Copy the contents of the `cattle-home` folder to your [CATTLE_HOME][1] directory and then restart Cattle.  If done properly you should have `CATTLE_HOME/etc/cattle/agents/pyagent/cattle/plugins/libvirt/custom_template.tmpl` and when the agent from the hypervisor connects you should see `/var/lib/cattle/pyagent/cattle/plugins/libvirt/custom_template.tmpl` on the hypervisor.

You can add whatever you want to the custom template, this example will setup a 9pfs share from `/var/lib/guest-shared-fs` and be available under the 9pfs tag shared-fs.  The share is by default read only.  Such a setup makes it really easy to develop something on the host system and then share the files with a VM.

**NOTE: Make sure you create the folder`/var/lib/guest-shared-fs` on the host**

[default_template.tmpl][2] is the base template, refer to that file to get an idea of what can be extended and changes.  Basically, everything should be configurable.

After you start/restart a VM using this configuration, refer to the data.libvirt.xml property of the virtual machine to ensure that your changes ended up in the libvirt domain XML.

  [1]: http://docs.cattle.io/en/latest/config/cattle-home.html
  [2]: https://github.com/cattleio/cattle/blob/master/code/agent/src/agents/pyagent/cattle/plugins/libvirt/default_template.tmpl