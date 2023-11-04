package org.yamcs.prometheus;

import static io.netty.handler.codec.http.HttpResponseStatus.TEMPORARY_REDIRECT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;

import org.yamcs.Plugin;
import org.yamcs.PluginException;
import org.yamcs.YConfiguration;
import org.yamcs.YamcsServer;
import org.yamcs.http.Handler;
import org.yamcs.http.HandlerContext;
import org.yamcs.http.HttpRequestHandler;
import org.yamcs.http.HttpServer;
import org.yamcs.logging.Log;
import org.yamcs.security.SystemPrivilege;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
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
        new ApiMetrics(httpServer.getMetricRegistry()).register(registry);

        if (config.getBoolean("jvm")) {
            JvmMetrics.builder().register(registry);
        }

        httpServer.addApi(new PrometheusApi(registry));

        // Prometheus by default expects a /metrics path.
        // Redirect it to /api/prometheus/metrics for convenience
        Handler redirectHandler = new RedirectHandler();
        httpServer.addHandler("metrics", () -> redirectHandler);
    }

    @Sharable
    private static final class RedirectHandler extends Handler {
        @Override
        public void handle(HandlerContext ctx) {
            var nettyCtx = ctx.getNettyChannelHandlerContext();
            var nettyRequest = ctx.getNettyFullHttpRequest();

            var response = new DefaultFullHttpResponse(HTTP_1_1, TEMPORARY_REDIRECT);
            var qs = new QueryStringDecoder(nettyRequest.uri());
            var location = qs.rawPath().replaceFirst("metrics", "api/prometheus/metrics");
            var q = qs.rawQuery();
            if (!q.isEmpty()) {
                location += "?" + q;
            }
            response.headers().add(HttpHeaderNames.LOCATION, location);
            HttpRequestHandler.sendResponse(nettyCtx, nettyRequest, response);
        }
    }
}
