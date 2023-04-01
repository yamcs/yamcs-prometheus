package org.yamcs.prometheus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.yamcs.YamcsServer;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;

public class LinkExports extends Collector {

    private static final List<String> LINK_LABELS = Arrays.asList(Labels.INSTANCE, Labels.LINK);

    @Override
    public List<MetricFamilySamples> collect() {
        var inCounters = new CounterMetricFamily(
                "yamcs_links_in_total",
                "Number of received items since Yamcs has started (e.g. packets)",
                LINK_LABELS);

        var outCounters = new CounterMetricFamily(
                "yamcs_links_out_total",
                "Number of sent items since Yamcs has started (e.g. telecommand packets)",
                LINK_LABELS);

        for (var instance : YamcsServer.getInstances()) {
            for (var link : instance.getLinkManager().getLinks()) {
                var labelValues = Arrays.asList(instance.getName(), link.getName());
                inCounters.addMetric(labelValues, link.getDataInCount());
                outCounters.addMetric(labelValues, link.getDataOutCount());
            }
        }

        var sampleFamilies = new ArrayList<MetricFamilySamples>();
        sampleFamilies.add(inCounters);
        sampleFamilies.add(outCounters);
        return sampleFamilies;
    }
}
