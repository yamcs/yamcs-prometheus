yamcs-prometheus
================

This is a Yamcs plugin for monitoring Yamcs performance using Prometheus.

More specifically this plugin adds an endpoint to the Yamcs HTTP API that can
be scraped by Prometheus or any other monitoring product supporting the
Prometheus metrics format (e.g. Zabbix).

Current exposed metrics include:

* Process-specific metrics (fds, start time, cpu time)
* Hotspot JVM metrics (GC, memory pools, classloading, thread counts)
* Version info of Yamcs and available plugins
* Yamcs Server ID
* Instance counts (total, and by-state)
* Link counters (in/out)
* Packet counters
* API request/error counters

The metrics output is human-readable and annotated with comments for each
metric.


Usage
-----

Add the following dependency to your Yamcs Maven project:

.. code-block:: xml

   <dependency>
     <groupId>org.yamcs</groupId>
     <artifactId>yamcs-prometheus</artifactId>
     <version>1.0.0</version>
   </dependency>


API
---

Metrics are available on the endpoint `/api/prometheus/metrics` on the standard
http or https port of your installation. If your installation is
password-protected, you will need to make a password-based user to be used by
Prometheus.

.. warning::
    Authenticated users require the privilege: `Prometheus.GetMetrics`.

For convenience, this plugin also binds to the `/metrics` endpoint. This is the
default location that Prometheus uses for configured targets. This will work,
but it is just a redirect to `/api/prometheus/metrics`. If you want to reduce
access logs, use the more specific endpoint.

Note that if Yamcs is hosted on a custom context path, then all mentioned paths
must be appropriately prefixed.

A copy-paste example for use in `prometheus.yml` is as follows:

.. code-block:: yaml

   scrape_configs:
     - job_name: yamcs
       metrics_path: /api/prometheus/metrics
       static_configs:
         - targets: ['localhost:8090']

For an authenticated setup on HTTPS, use rather something like this:

.. code-block:: yaml

   scrape_configs:
     - job_name: yamcs
       scheme: https
       basic_auth:
         username: prometheus
         password: 'password'
       metrics_path: /api/prometheus/metrics
       static_configs:
         - targets: ['localhost:8090']


Status
------

* This plugin is experimental.
* This plugin is maintained by the Yamcs Team at Space Applications Services.


Example Scrape
--------------

.. literalinclude:: scrape.txt
