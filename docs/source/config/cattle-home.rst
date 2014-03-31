Cattle Home
===========

Extensions, logs, and the embedded database are by default stored in :file:`$CATTLE_HOME`.  Also, :file:`${CATTLE_HOME}/etc/cattle` is automatically added to the classpath, so :file:`${CATTLE_HOME}/etc/cattle/cattle.properties` will be found as the cattle config file and read.

If you are running the :file:`cattle.jar` manually from the command line, :file:`$CATTLE_HOME` defaults to :file:`${HOME}/.cattle`.

If you are running Cattle from the docker image "cattle/server," the :file:`$CATTLE_HOME` is by default :file:`/var/lib/cattle`.  If you want to customize Cattle while running docker you have two options (and plenty more you could think of).

1. **Build a custom image**:  You can always build a custom image that packages your contents of :file:`$CATTLE_HOME`, for example::

    FROM cattle/server
    ADD cattle-home /var/lib/cattle

2. **Map a local folder to /var/lib/cattle**:  You can also just map a local folder to :file:`/var/lib/cattle`, for example: ``docker run -v $(pwd)/cattle-home:/var/lib/cattle cattle/server``
