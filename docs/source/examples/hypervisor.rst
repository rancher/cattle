.. _example_hypervisor:

Adding a hypervisor
===================

Add a new hypervisor is relatively straight forward.  You just need to subscribe to and handle 7 agent events.  Refer to the inline comments in https://github.com/cattleio/cattle/blob/master/docs/examples/handler-bash/hypervisor.sh.  The events you must handle are as follows:

**storage.image.activate**:  Download and make the image ready for use on this hypervisor.

**storage.image.remove**:  Remove the specified image.  All volumes associated to this image will have already been deleted from the hypervisor.

**storage.volume.activate**: Create or enable the volume.  If the volume already exists, perform any other actions that may be necessary right before an instance starts using the volume.

**storage.volume.deactivate**: Called after an instance is shutdown and the volume is not in use anymore.  The volume should not be deleted, just deactivated.  For many volume formats there is nothing to do for deactivate.

**storage.volume.remove**: Remove the specified volume.

**compute.instance.activate**: Turn on the instance (virtual machine or container).  You can assume that the storage is already created and available.

**compute.instance.deactivate**: Turn off the instance.
