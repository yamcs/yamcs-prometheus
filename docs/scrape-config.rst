Scrape Configuration
====================

In case you are using a Prometheus server to scrape metrics,
you need to update appropriate scrape configuration in its 
``prometheus.yml`` configuration file.

Adapt from the following example:

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
