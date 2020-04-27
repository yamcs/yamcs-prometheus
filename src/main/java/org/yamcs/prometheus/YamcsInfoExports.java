package org.yamcs.prometheus;

import java.util.ArrayList;
import java.util.List;

import org.yamcs.Plugin;
import org.yamcs.PluginManager;
import org.yamcs.PluginMetadata;
import org.yamcs.YamcsServer;
import org.yamcs.YamcsVersion;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

public class YamcsInfoExports extends Collector {

    private PluginManager pluginManager;

    public YamcsInfoExports() {
        pluginManager = YamcsServer.getServer().getPluginManager();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();

        List<String> labelNames = new ArrayList<>();
        labelNames.add("version");
        labelNames.add("revision");
        labelNames.add("server_id");

        List<String> labelValues = new ArrayList<>();
        labelValues.add(YamcsVersion.VERSION);
        labelValues.add(YamcsVersion.REVISION);
        labelValues.add(YamcsServer.getServer().getServerId());

        for (Plugin plugin : pluginManager.getPlugins()) {
            PluginMetadata meta = pluginManager.getMetadata(plugin.getClass());
            labelNames.add(Collector.sanitizeMetricName(meta.getName()) + "_version");
            labelValues.add(meta.getVersion());
        }

        GaugeMetricFamily serverInfo = new GaugeMetricFamily(
                "yamcs_info",
                "Version info",
                labelNames);

        serverInfo.addMetric(labelValues, 1);
        mfs.add(serverInfo);

        return mfs;
    }
}
