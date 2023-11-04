package org.yamcs.prometheus;

import org.yamcs.YamcsServer;
import org.yamcs.management.ManagementGpbHelper;

import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class ProcessorMetrics {

    private static final String[] PACKET_LABELS = new String[] { Labels.INSTANCE, Labels.PROCESSOR, Labels.PACKET };

    public void register(PrometheusRegistry registry) {
        CounterWithCallback.builder()
                .name("yamcs_processor_receive_packets_total")
                .help("Processed packets since Yamcs has started")
                .labelNames(PACKET_LABELS)
                .callback(cb -> {
                    for (var yamcsInstance : YamcsServer.getInstances()) {
                        for (var processor : yamcsInstance.getProcessors()) {
                            if (processor.isPersistent()) {
                                var processorStats = processor.getTmProcessor().getStatistics();
                                var stats = ManagementGpbHelper.buildStats(processor, processorStats.snapshot());

                                for (var packetStats : stats.getTmstatsList()) {
                                    cb.call(packetStats.getReceivedPackets(),
                                            processor.getInstance(), processor.getName(), packetStats.getPacketName());
                                }
                            }
                        }
                    }
                })
                .register(registry);

        // Note: avoid exposing rates. Consumers can calculate these too.
        // Example: 5s moving average in Prometheus: rate(yamcs_processor_receive_packets_total[5s])
    }
}
