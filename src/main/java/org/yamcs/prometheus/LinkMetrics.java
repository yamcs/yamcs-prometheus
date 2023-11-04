package org.yamcs.prometheus;

import org.yamcs.YamcsServer;

import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class LinkMetrics {

    private static final String[] LINK_LABELS = new String[] { Labels.INSTANCE, Labels.LINK };

    public void register(PrometheusRegistry registry) {
        CounterWithCallback.builder()
                .name("yamcs_links_in")
                .help("Number of received items since Yamcs has started (e.g. packets)")
                .labelNames(LINK_LABELS)
                .callback(cb -> {
                    for (var instance : YamcsServer.getInstances()) {
                        for (var link : instance.getLinkManager().getLinks()) {
                            cb.call(link.getDataInCount(), instance.getName(), link.getName());
                        }
                    }
                })
                .register(registry);

        CounterWithCallback.builder()
                .name("yamcs_links_out")
                .help("Number of sent items since Yamcs has started (e.g. telecommand packets)")
                .labelNames(LINK_LABELS)
                .callback(cb -> {
                    for (var instance : YamcsServer.getInstances()) {
                        for (var link : instance.getLinkManager().getLinks()) {
                            cb.call(link.getDataOutCount(), instance.getName(), link.getName());
                        }
                    }
                })
                .register(registry);
    }
}
