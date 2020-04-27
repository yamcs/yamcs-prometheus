package org.yamcs.prometheus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.yamcs.Processor;
import org.yamcs.YamcsServer;
import org.yamcs.YamcsServerInstance;
import org.yamcs.management.ManagementGpbHelper;
import org.yamcs.protobuf.Statistics;
import org.yamcs.protobuf.TmStatistics;
import org.yamcs.xtceproc.ProcessingStatistics;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;

public class ProcessorExports extends Collector {

    private static final List<String> PACKET_LABELS = Arrays.asList("instance", "processor", "packet");

    @Override
    public List<MetricFamilySamples> collect() {
        CounterMetricFamily receivedPackets = new CounterMetricFamily(
                "yamcs_processor_receive_packets_total",
                "Processed packets since Yamcs has started",
                PACKET_LABELS);

        /*CounterMetricFamily receivedBytes = new CounterMetricFamily(
                "yamcs_processor_receive_bytes_total",
                "Processed bytes since Yamcs has started",
                PACKET_LABELS);*/

        // Note: avoid exposing rates. Consumers can calculate these too.
        // Example: 5s moving average in Prometheus: rate(yamcs_processor_receive_packets_total[5s])

        for (YamcsServerInstance instance : YamcsServer.getInstances()) {
            for (Processor processor : instance.getProcessors()) {
                if (processor.isPersistent()) {
                    ProcessingStatistics ps = processor.getTmProcessor().getStatistics();
                    Statistics stats = ManagementGpbHelper.buildStats(processor, ps.snapshot());

                    for (TmStatistics packetStats : stats.getTmstatsList()) {
                        List<String> labelValues = Arrays.asList(
                                processor.getInstance(), processor.getName(), packetStats.getPacketName());
                        receivedPackets.addMetric(labelValues, packetStats.getReceivedPackets());
                    }
                }
            }
        }

        List<MetricFamilySamples> sampleFamilies = new ArrayList<>();
        sampleFamilies.add(receivedPackets);
        return sampleFamilies;
    }
}
