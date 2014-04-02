Customizing Libvirt Configuration
=================================

Launching a virtual machine using libvirt is largely determined by the domain XML that is passed to libvirt.  The domain XML is created using `Mako templates <http://www.makotemplates.org/>`_ to make it very easy to tweak.  The default libvirt XML template is setup with blocks designed to be customized using Mako's `inheritance <http://docs.makotemplates.org/en/latest/inheritance.html>`_.  This way you can easily package and use your own custom libvirt domain XML template that overrides a section of the configuration and adds items specific to your setup.  Refer to https://github.com/cattleio/cattle/tree/master/docs/examples/libvirt/hostfs for more information.
