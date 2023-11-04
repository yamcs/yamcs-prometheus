package org.yamcs.prometheus;

import java.io.IOException;
import java.util.function.Predicate;

import org.yamcs.api.HttpBody;
import org.yamcs.api.Observer;
import org.yamcs.http.Context;
import org.yamcs.prometheus.api.AbstractPrometheusApi;
import org.yamcs.prometheus.api.GetMetricsRequest;

import com.google.protobuf.ByteString;

import io.prometheus.metrics.expositionformats.ExpositionFormats;
import io.prometheus.metrics.model.registry.MetricNameFilter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

/**
 * Responds to HTTP requests with current metrics in the text-based Prometheus exposition format v0.0.4
 * 
 * @see https://prometheus.io/docs/instrumenting/exposition_formats/
 */
public class PrometheusApi extends AbstractPrometheusApi<Context> {

    private PrometheusRegistry registry;
    private ExpositionFormats formats;

    public PrometheusApi(PrometheusRegistry registry) {
        this.registry = registry;
        formats = ExpositionFormats.init();
    }

    @Override
    public void getMetrics(Context ctx, GetMetricsRequest request, Observer<HttpBody> observer) {
        ctx.checkSystemPrivilege(PrometheusPlugin.PRIV_GET_METRICS);

        Predicate<String> filter = null;
        if (!request.getNameList().isEmpty()) {
            filter = MetricNameFilter.builder().nameMustBeEqualTo(request.getNameList()).build();
        }

        var metricSnapshots = registry.scrape(filter);

        var formatWriter = formats.getPrometheusTextFormatWriter();
        try (var out = ByteString.newOutput()) {
            formatWriter.write(out, metricSnapshots);
            out.close();

            var body = HttpBody.newBuilder()
                    .setContentType(formatWriter.getContentType())
                    .setData(out.toByteString())
                    .build();
            observer.complete(body);
        } catch (IOException e) {
            observer.completeExceptionally(e);
        }
    }
}
