package org.yamcs.prometheus;

import static io.prometheus.metrics.model.snapshots.PrometheusNaming.sanitizeMetricName;

import java.util.ArrayList;

import org.yamcs.YamcsServer;
import org.yamcs.YamcsVersion;

import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class YamcsInfoMetrics {

    public void register(PrometheusRegistry registry) {
        var pluginManager = YamcsServer.getServer().getPluginManager();

        var labelNames = new ArrayList<String>();
        labelNames.add("version");
        labelNames.add("revision");
        labelNames.add("server_id");

        var labelValues = new ArrayList<String>();
        labelValues.add(YamcsVersion.VERSION);
        labelValues.add(YamcsVersion.REVISION);
        labelValues.add(YamcsServer.getServer().getServerId());

        for (var plugin : pluginManager.getPlugins()) {
            var pluginMeta = pluginManager.getMetadata(plugin.getClass());
            labelNames.add(sanitizeMetricName(pluginMeta.getName()) + "_version");
            labelValues.add(pluginMeta.getVersion());
        }

        var info = Info.builder()
                .name("yamcs")
                .help("Yamcs version info")
                .labelNames(labelNames.toArray(new String[0]))
                .register(registry);

        info.setLabelValues(labelValues.toArray(new String[0]));
    }
}
