#!/usr/bin/env bash

# stop all existed container
se(){
    docker ps -a | grep "Exited" | awk '{print $1 }'|xargs docker stop
}

# remove all stopped and existed container
re(){
    docker ps -a | grep "Exited" | awk '{print $1 }'|xargs docker rm
}

# remove images, and default to remove none status images
ri(){
    docker images|grep none|awk '{print $3 }'|xargs docker rmi
}


case "$1" in
    "se")   se ;;
    "re")    re ;;
    "ri")   ri  ;;
esac

echo
echo "done!"
echo

exit