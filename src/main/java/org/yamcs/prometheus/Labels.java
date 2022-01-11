package org.yamcs.prometheus;

public interface Labels {

    /**
     * The name of a Yamcs instance. This uses the label {@code yamcs_instance} instead of {@code instance} because
     * the latter is automatically added by Prometheus with/ a different meaning.
     */
    String INSTANCE = "yamcs_instance";

    /**
     * Short name of a link. Links move data in and out of Yamcs. Unique within an {@link INSTANCE}
     */
    String LINK = "link";

    /**
     * Name of an API method. Unique within a {@link SERVICE}
     */
    String METHOD = "method";

    /**
     * Name of a packet. This usually matches the short name of the first non-abstract container.
     */
    String PACKET = "packet";

    /**
     * Name of a processor. Unique within an {@link INSTANCE}
     */
    String PROCESSOR = "processor";

    /**
     * A service wraps a collection of related API methods. This is not related to guava services.
     */
    String SERVICE = "service";
}
