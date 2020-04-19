package org.yamcs.prometheus;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import org.yamcs.api.HttpBody;
import org.yamcs.api.Observer;
import org.yamcs.http.Context;
import org.yamcs.prometheus.api.AbstractPrometheusApi;
import org.yamcs.prometheus.api.GetMetricsRequest;

import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.Output;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

/**
 * Responds to HTTP requests with current metrics in the text-based Prometheus exposition format v0.0.4
 * 
 * @see https://prometheus.io/docs/instrumenting/exposition_formats/
 */
public class PrometheusApi extends AbstractPrometheusApi<Context> {

    private CollectorRegistry registry;

    public PrometheusApi(CollectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void getMetrics(Context ctx, GetMetricsRequest request, Observer<HttpBody> observer) {
        ctx.checkSystemPrivilege(PrometheusPlugin.PRIV_GET_METRICS);

        Set<String> names = new HashSet<>(request.getNameList());

        try (Output out = ByteString.newOutput(); Writer writer = new OutputStreamWriter(out)) {
            TextFormat.write004(writer, registry.filteredMetricFamilySamples(names));
            writer.close();

            HttpBody body = HttpBody.newBuilder()
                    .setContentType(TextFormat.CONTENT_TYPE_004)
                    .setData(out.toByteString())
                    .build();
            observer.complete(body);
        } catch (IOException e) {
            observer.completeExceptionally(e);
        }
    }
}
