Configuring Cattle
==================

The configuration of Cattle is very flexible.  Under the hood it is based on `Netflix's Archaius <https://github.com/Netflix/archaius>`_ which is in turn based on `Apache Commons Configuration <https://github.com/Netflix/archaius>`_.  The configuration of Cattle is essentially key value based.  The key values pairs will be read from the following sources.  If the configuration is found in the source it will not continue to look for it in further sources.  The order is as below:

1. Environment variables
2. Java system properties
3. :file:`cattle-local.properties` on the classpath
4. :file:`cattle.properties` on the classpath
5. The database from cattle.setting table

The current configuration can be viewed from and modified from the API at http://localhost:8080/v1/settings.  Any changes done using the API will saved in the database and immediately refreshed on all Cattle services.  If you have overridden the settings using environment varibles, Java system properties, or local config files, the changes to the database will not matter as the configuration is not coming from the database.  Refer to the "source" property in the API to determine from where the configuration is being read.

Environment Variables
*********************

Environment variables have a special format.  Configuration keys in Cattle are dot separated names like ``db.cattle.database``.  Environment variables can not have dots in their name so ``.`` is replaced by ``_``.  Additionally, in order to not read all the environment variables available and only read the environment variables specific to Cattle, the environment variable must start with ``CATTLE_``.  The ``CATTLE_`` prefix will be stripped.  To sum this all up, ``db.cattle.database``, if set through an environment variable, must be ``CATTLE_DB_CATTLE_DATABASE``.
