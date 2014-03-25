#!/bin/bash
set -e -x

cd $(dirname $0)

INSTANCE_ID=../before.d/ec2-id
KEY=../before.d/key
SSH_ARGS="-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $KEY -l root"

get_dns()
{
    aws ec2 describe-instances --instance-ids $(<$INSTANCE_ID) \
                               --query 'Reservations[0].Instances[0].PublicDnsName' \
                               --output text
}

if [ "$EC2_TEST" != "true" ] || [ ! -e $INSTANCE_ID ]; then
    exit 0
fi

while [ "$(get_dns)" = "" ]; do
    echo Waiting for EC2
    sleep 1
done

DNS=$(get_dns)

while ! ssh $SSH_ARGS $DNS -- test -e /var/tmp/setup-done; do
    echo 'Waiting for initial setup'
    sleep 2
done

trap '[ -e authorized_keys_tmp ] && rm authorized_keys_tmp' EXIT
curl -s http://localhost:8080/v1/authorized_keys > authorized_keys_tmp
mv authorized_keys_tmp authorized_keys


cat authorized_keys | ssh $SSH_ARGS \
                          $DNS --   \
                          tee -a /root/.ssh/authorized_keys

curl -X POST http://localhost:8080/v1/agents -F uri="ssh://root@${DNS}:22" -F uuid=ec2-host1

cat > $(basename $0)-integration-env << EOF
export DOCKER_AGENT_UUID=ec2-host1
export LIBVIRT_AGENT_UUID=ec2-host1
EOF

rsync -az --delete -e "ssh $SSH_ARGS" ../../../ $DNS:/usr/src/dstack
ssh $SSH_ARGS $DNS -- /usr/src/dstack/tools/build/runtests-agent.sh
