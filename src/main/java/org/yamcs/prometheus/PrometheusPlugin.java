package org.yamcs.prometheus;

import java.io.IOException;

import org.yamcs.Plugin;
import org.yamcs.PluginException;
import org.yamcs.YConfiguration;
import org.yamcs.YamcsServer;
import org.yamcs.http.HandlerContext;
import org.yamcs.http.HttpHandler;
import org.yamcs.http.HttpServer;
import org.yamcs.logging.Log;
import org.yamcs.security.SystemPrivilege;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class PrometheusPlugin implements Plugin {

    public static final SystemPrivilege PRIV_GET_METRICS = new SystemPrivilege("Prometheus.GetMetrics");

    private static final Log log = new Log(PrometheusPlugin.class);

    @Override
    public void onLoad(YConfiguration config) throws PluginException {
        var yamcs = YamcsServer.getServer();
        yamcs.getSecurityStore().addSystemPrivilege(PRIV_GET_METRICS);

        var httpServer = yamcs.getGlobalService(HttpServer.class);
        if (httpServer == null) {
            log.warn("Can't mount metrics endpoint. Yamcs does not appear to be running an HTTP Server.");
            return;
        }

        try (var in = getClass().getResourceAsStream("/yamcs-prometheus.protobin")) {
            httpServer.getProtobufRegistry().importDefinitions(in);
        } catch (IOException e) {
            throw new PluginException(e);
        }

        var registry = PrometheusRegistry.defaultRegistry;
        new YamcsInfoMetrics().register(registry);
        new InstancesMetrics().register(registry);
        new LinkMetrics().register(registry);
        new ProcessorMetrics().register(registry);
        new HttpMetrics().register(registry);
        new ApiMetrics().register(registry);

        if (config.getBoolean("jvm")) {
            JvmMetrics.builder().register(registry);
        }

        httpServer.addApi(new PrometheusApi(registry));

        // Prometheus by default expects a /metrics path.
        // Redirect it to /api/prometheus/metrics for convenience
        var redirectHandler = new RedirectHandler();
        httpServer.addRoute("metrics", () -> redirectHandler);
    }

    @Sharable
    private static final class RedirectHandler extends HttpHandler {

        @Override
        public boolean requireAuth() {
            return true;
        }

        @Override
        public void handle(HandlerContext ctx) {
            var nettyRequest = ctx.getNettyHttpRequest();
            var qs = new QueryStringDecoder(nettyRequest.uri());
            var location = qs.rawPath().replaceFirst("metrics", "api/prometheus/metrics");
            var q = qs.rawQuery();
            if (!q.isEmpty()) {
                location += "?" + q;
            }
            ctx.sendRedirect(location);
        }
    }
}
