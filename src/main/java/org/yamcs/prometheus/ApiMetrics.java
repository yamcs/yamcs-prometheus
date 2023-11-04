package org.yamcs.prometheus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class ApiMetrics {

    private static final String[] LABEL_NAMES = new String[] { Labels.SERVICE, Labels.METHOD };
    private static final MetricFilter FILTER_REQUESTS = MetricFilter.startsWith("yamcs.api.requests.total");
    private static final MetricFilter FILTER_ERRORS = MetricFilter.startsWith("yamcs.api.errors.total");

    private MetricRegistry metricRegistry;
    private ConcurrentMap<String, String[]> labelsForMetric = new ConcurrentHashMap<>();

    public ApiMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void register(PrometheusRegistry registry) {
        CounterWithCallback.builder()
                .name("yamcs_api_requests")
                .help("Total requests per API method")
                .labelNames(LABEL_NAMES)
                .callback(cb -> {
                    var counters = metricRegistry.getCounters(FILTER_REQUESTS);
                    for (var entry : counters.entrySet()) {
                        String dropwizardName = entry.getKey();
                        Counter counter = entry.getValue();
                        var labelValues = labelsForMetric.computeIfAbsent(dropwizardName, this::getRouteLabels);
                        cb.call(counter.getCount(), labelValues);
                    }
                })
                .register(registry);

        CounterWithCallback.builder()
                .name("yamcs_api_errors")
                .help("Total errors per API method")
                .labelNames(LABEL_NAMES)
                .callback(cb -> {
                    var counters = metricRegistry.getCounters(FILTER_ERRORS);
                    for (var entry : counters.entrySet()) {
                        String dropwizardName = entry.getKey();
                        Counter counter = entry.getValue();
                        var labelValues = labelsForMetric.computeIfAbsent(dropwizardName, this::getRouteLabels);
                        cb.call(counter.getCount(), labelValues);
                    }
                })
                .register(registry);
    }

    private String[] getRouteLabels(String dropwizardName) {
        var parts = dropwizardName.split("\\.");
        return new String[] { parts[4], parts[5] };
    }
}
