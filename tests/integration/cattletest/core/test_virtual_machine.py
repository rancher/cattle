from common_fixtures import *  # NOQA


def test_virtual_machine_create_cpu_memory(client, sim_context):
    image_uuid = sim_context['imageUuid']
    vm = client.create_virtual_machine(imageUuid=image_uuid,
                                       vcpu=2,
                                       memoryMb=42)

    vm = client.wait_success(vm)
    assert vm.state == 'running'

    assert vm.vcpu == 2
    assert vm.memoryMb == 42


def test_virtual_machine_create(client, sim_context):
    image_uuid = sim_context['imageUuid']
    vm = client.create_virtual_machine(imageUuid=image_uuid)

    vm = client.wait_success(vm)
    assert vm.state == 'running'

    assert vm.vcpu is None
    assert vm.memoryMb is None
