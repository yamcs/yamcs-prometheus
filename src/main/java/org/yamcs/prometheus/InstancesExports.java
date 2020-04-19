package org.yamcs.prometheus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yamcs.YamcsServer;
import org.yamcs.YamcsServerInstance;
import org.yamcs.protobuf.YamcsInstance.InstanceState;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

public class InstancesExports extends Collector {

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> sampleFamilies = new ArrayList<>();

        sampleFamilies.add(new GaugeMetricFamily(
                "instances_current",
                "Current amount of Yamcs instances",
                YamcsServer.getInstances().size()));

        GaugeMetricFamily stateFamily = new GaugeMetricFamily(
                "instances_state",
                "Current count of instances by state",
                Collections.singletonList("state"));

        Map<InstanceState, Integer> stateCounts = getStateCountMap();
        for (Map.Entry<InstanceState, Integer> entry : stateCounts.entrySet()) {
            stateFamily.addMetric(
                    Collections.singletonList(entry.getKey().toString()),
                    entry.getValue());
        }
        sampleFamilies.add(stateFamily);

        return sampleFamilies;
    }

    private Map<InstanceState, Integer> getStateCountMap() {
        Map<InstanceState, Integer> counts = new HashMap<>();
        for (InstanceState state : InstanceState.values()) {
            counts.put(state, 0);
        }

        for (YamcsServerInstance instance : YamcsServer.getInstances()) {
            InstanceState state = instance.state();
            counts.put(state, counts.get(state) + 1);
        }

        return counts;
    }
}
