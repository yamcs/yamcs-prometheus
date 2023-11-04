package org.yamcs.prometheus;

import org.yamcs.YamcsServer;
import org.yamcs.http.HttpServer;

import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.Unit;

public class HttpMetrics {

    private HttpServer httpServer;

    public HttpMetrics() {
        httpServer = YamcsServer.getServer().getGlobalService(HttpServer.class);
    }

    public void register(PrometheusRegistry registry) {
        CounterWithCallback.builder()
                .name("yamcs_http_receive")
                .unit(Unit.BYTES)
                .callback(cb -> {
                    var globalTrafficHandler = httpServer.getGlobalTrafficShapingHandler();
                    if (globalTrafficHandler != null) {
                        var trafficCounter = globalTrafficHandler.trafficCounter();
                        cb.call(trafficCounter.cumulativeReadBytes());
                    }
                })
                .register(registry);

        CounterWithCallback.builder()
                .name("yamcs_http_transmit")
                .unit(Unit.BYTES)
                .callback(cb -> {
                    var globalTrafficHandler = httpServer.getGlobalTrafficShapingHandler();
                    if (globalTrafficHandler != null) {
                        var trafficCounter = globalTrafficHandler.trafficCounter();
                        cb.call(trafficCounter.cumulativeWrittenBytes());
                    }
                })
                .register(registry);

        GaugeWithCallback.builder()
                .name("yamcs_http_connections")
                .callback(cb -> {
                    cb.call(httpServer.getClientChannels().size());
                })
                .register(registry);

        // Note: avoid exposing rates. Consumers can calculate these from the counters.
    }
}
