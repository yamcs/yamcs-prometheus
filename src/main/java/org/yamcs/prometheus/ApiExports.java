package org.yamcs.prometheus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.dropwizard.samplebuilder.CustomMappingSampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;

public class ApiExports extends Collector {

    private static final String REQUESTS_NAME_PREFIX = "yamcs.api.requests.total";
    private static final String ERRORS_NAME_PREFIX = "yamcs.api.errors.total";

    private MetricRegistry registry;
    private SampleBuilder sampleBuilder;

    public ApiExports(MetricRegistry registry) {
        this.registry = registry;
        List<MapperConfig> mappings = new ArrayList<>();

        Map<String, String> labels = new HashMap<>();
        labels.put(Labels.SERVICE, "${0}");
        labels.put(Labels.METHOD, "${1}");

        MapperConfig mapping = new MapperConfig();
        mapping.setMatch("yamcs.api.requests.total.*.*");
        mapping.setName(REQUESTS_NAME_PREFIX);
        mapping.setLabels(labels);
        mappings.add(mapping);

        mapping = new MapperConfig();
        mapping.setMatch("yamcs.api.errors.total.*.*");
        mapping.setName(ERRORS_NAME_PREFIX);
        mapping.setLabels(labels);
        mappings.add(mapping);

        sampleBuilder = new CustomMappingSampleBuilder(mappings);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        Map<String, MetricFamilySamples> samplesMap = new HashMap<>();
        for (Entry<String, Counter> entry : registry.getCounters().entrySet()) {
            String dropwizardName = entry.getKey();
            Counter counter = entry.getValue();

            Sample sample = sampleBuilder.createSample(dropwizardName, "", new ArrayList<String>(),
                    new ArrayList<String>(),
                    Long.valueOf(counter.getCount()).doubleValue());

            addToMap(samplesMap, new MetricFamilySamples(sample.name, Type.COUNTER,
                    getHelpMessage(dropwizardName),
                    Arrays.asList(sample)));
        }
        return new ArrayList<>(samplesMap.values());
    }

    private String getHelpMessage(String dropwizardName) {
        if (dropwizardName.startsWith(REQUESTS_NAME_PREFIX)) {
            return "Total requests per API method";
        }
        if (dropwizardName.startsWith(ERRORS_NAME_PREFIX)) {
            return "Total errors per API method";
        }
        return "";
    }

    private void addToMap(Map<String, MetricFamilySamples> map, MetricFamilySamples newSamples) {
        if (newSamples != null) {
            MetricFamilySamples currentSamples = map.get(newSamples.name);
            if (currentSamples == null) {
                map.put(newSamples.name, newSamples);
            } else {
                List<Sample> samples = new ArrayList<>(currentSamples.samples);
                samples.addAll(newSamples.samples);
                map.put(newSamples.name, new MetricFamilySamples(newSamples.name, currentSamples.type,
                        currentSamples.help, samples));
            }
        }
    }
}
