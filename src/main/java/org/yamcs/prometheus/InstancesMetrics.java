package org.yamcs.prometheus;

import java.util.HashMap;
import java.util.Map;

import org.yamcs.YamcsServer;
import org.yamcs.protobuf.YamcsInstance.InstanceState;

import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class InstancesMetrics {

    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("yamcs_instances_current")
                .help("Current amount of Yamcs instances")
                .callback(cb -> cb.call(YamcsServer.getInstances().size()))
                .register(registry);

        GaugeWithCallback.builder()
                .name("yamcs_instances_state")
                .help("Current count of instances by state")
                .labelNames("state")
                .callback(cb -> {
                    var stateCounts = getStateCountMap();
                    for (var entry : stateCounts.entrySet()) {
                        cb.call(entry.getValue(), entry.getKey().toString());
                    }
                })
                .register(registry);
    }

    private Map<InstanceState, Integer> getStateCountMap() {
        Map<InstanceState, Integer> counts = new HashMap<>();
        for (var state : InstanceState.values()) {
            counts.put(state, 0);
        }

        for (var instance : YamcsServer.getInstances()) {
            var state = instance.state();
            counts.put(state, counts.get(state) + 1);
        }

        return counts;
    }
}
