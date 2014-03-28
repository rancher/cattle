.. _docker-example:

Docker Example
==============

To get a basic overview of how Docker works we will use Docker to deploy a simple multi-tiered application.

.. image:: https://docs.google.com/drawings/d/1jXfAGAb2h0oYGZlRh-ihFWGp2bPvI4M2drOaV3ESrHc/pub?w=402&h=219
   :align: center

The application will we be deploying is Cattle itself and additionally log aggregation with Logstash/Elasticsearch and Kibana for the UI.  The full script to run all of the below commands is `cattle-scripts.zip download <https://github.com/cattleio/cattle/tree/master#2-download>`_ in docker-example.sh.

Launch Logstash/Elastisearch
****************************

We're going to launch logstash with Elasticsearch embedded.

.. code-block:: bash

    docker run -d --name logstash cattle/logstash


Launch Kibana
*************

We will now launch Kibana and link it to the embedded Elasticsearch.

.. code-block:: bash

    docker run -d -p 80 --name kibana --link logstash:es cattle/kibana

You can run :command:`docker ps` to see which port Kibana is running on.  You can use gist https://gist.github.com/cattleio/5d0ae8b4dd408ff8181a as a sample dashboard.

.. image:: kibana-gist.png
   :align: center
   :width: 25%

Launch MySQL
************

.. code-block:: bash

    docker run -d -p 80 --name mysql cattle/mysql

You can run :command:`docker ps` to see which port phpMyAdmin is running on.  Hit http://localhost:PORT/phpmyadmin and you can login with cattle/cattle for the account.

Launch Cattle
*************

Now launch Cattle linking it to MySQL and Logstash (using GELF).

.. code-block:: bash

    docker run -d -p 8080 --name cattle --link mysql:mysql --link logstash:gelf cattle/cattle

You can run :command:`docker ps` to see which port Cattle is running on.
