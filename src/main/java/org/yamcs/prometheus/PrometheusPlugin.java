package org.yamcs.prometheus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.yamcs.Plugin;
import org.yamcs.PluginException;
import org.yamcs.Spec;
import org.yamcs.Spec.OptionType;
import org.yamcs.YConfiguration;
import org.yamcs.YamcsServer;
import org.yamcs.http.HttpServer;
import org.yamcs.logging.Log;
import org.yamcs.security.SystemPrivilege;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;

public class PrometheusPlugin implements Plugin {

    public static final SystemPrivilege PRIV_GET_METRICS = new SystemPrivilege("Prometheus.GetMetrics");

    private static final Log log = new Log(PrometheusPlugin.class);
    private static final String CONFIG_SECTION = "yamcs-prometheus";

    private YamcsServer yamcs;

    public PrometheusPlugin() {
        yamcs = YamcsServer.getServer();

        Spec spec = new Spec();
        spec.addOption("jvm", OptionType.BOOLEAN).withDefault(Boolean.TRUE);

        yamcs.addConfigurationSection(CONFIG_SECTION, spec);
    }

    @Override
    public void onLoad() throws PluginException {
        yamcs.getSecurityStore().addSystemPrivilege(PRIV_GET_METRICS);

        YConfiguration yamcsConfig = yamcs.getConfig();
        YConfiguration config = YConfiguration.emptyConfig();
        if (yamcsConfig.containsKey(CONFIG_SECTION)) {
            config = yamcsConfig.getConfig(CONFIG_SECTION);
        }

        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        if (config.getBoolean("jvm")) {
            DefaultExports.register(registry);
        }
        new VersionInfoExports().register(registry);
        new InstancesExports().register(registry);

        List<HttpServer> httpServers = yamcs.getGlobalServices(HttpServer.class);
        if (httpServers.isEmpty()) {
            log.warn("Can't mount metrics endpoint. Yamcs does not appear to be running an HTTP Server.");
            return;
        }

        for (HttpServer httpServer : httpServers) {
            try (InputStream in = getClass().getResourceAsStream("/yamcs-prometheus.protobin")) {
                httpServer.getProtobufRegistry().importDefinitions(in);
            } catch (IOException e) {
                throw new PluginException(e);
            }

            httpServer.addApi(new PrometheusApi(registry));
        }
    }
}
