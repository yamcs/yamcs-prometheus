package org.yamcs.prometheus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.yamcs.YamcsServer;
import org.yamcs.YamcsServerInstance;
import org.yamcs.protobuf.LinkInfo;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;

public class LinkExports extends Collector {

    private static final List<String> LINK_LABELS = Arrays.asList(Labels.INSTANCE, Labels.LINK);

    @Override
    public List<MetricFamilySamples> collect() {
        CounterMetricFamily inCounters = new CounterMetricFamily(
                "yamcs_links_in_total",
                "Number of received items since Yamcs has started (e.g. packets)",
                LINK_LABELS);

        CounterMetricFamily outCounters = new CounterMetricFamily(
                "yamcs_links_out_total",
                "Number of sent items since Yamcs has started (e.g. telecommand packets)",
                LINK_LABELS);

        for (YamcsServerInstance instance : YamcsServer.getInstances()) {
            for (LinkInfo link : instance.getLinkManager().getLinkInfo()) {
                List<String> labelValues = Arrays.asList(link.getInstance(), link.getName());
                inCounters.addMetric(labelValues, link.getDataInCount());
                outCounters.addMetric(labelValues, link.getDataOutCount());
            }
        }

        List<MetricFamilySamples> sampleFamilies = new ArrayList<>();
        sampleFamilies.add(inCounters);
        sampleFamilies.add(outCounters);
        return sampleFamilies;
    }
}
