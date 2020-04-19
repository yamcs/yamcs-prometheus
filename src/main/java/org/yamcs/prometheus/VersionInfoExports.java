package org.yamcs.prometheus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.yamcs.YamcsServer;
import org.yamcs.YamcsVersion;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

public class VersionInfoExports extends Collector {

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();

        List<String> labelNames = Arrays.asList("version", "revision", "server_id");
        List<String> labelValues = Arrays.asList(
                YamcsVersion.VERSION,
                YamcsVersion.REVISION,
                YamcsServer.getServer().getServerId());

        GaugeMetricFamily serverInfo = new GaugeMetricFamily(
                "info",
                "Version info",
                labelNames);

        serverInfo.addMetric(labelValues, 1);
        mfs.add(serverInfo);

        return mfs;
    }
}
