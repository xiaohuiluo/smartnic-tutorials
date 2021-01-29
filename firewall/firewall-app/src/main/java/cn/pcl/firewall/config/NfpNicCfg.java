package cn.pcl.firewall.config;

import cn.pcl.firewall.NfpNicDevice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.internal.guava.Sets;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigException;

import java.util.Set;

import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;

public class NfpNicCfg extends Config<ApplicationId> {
    public static final String DP_ID = "dp_id";

    public static final String DRIVER = "driver";

    public static final String RTE_HOST = "rte_host";

    public static final String RTE_PORT = "rte_port";

    private static final String CONFIG_VALUE_ERROR = "Error parsing config value";

    @Override
    public boolean isValid() {
        for (JsonNode node : array) {
            if (!(hasOnlyFields((ObjectNode) node, DP_ID, DRIVER, RTE_HOST, RTE_PORT))) {
                return false;
            }

            JsonNode driver = node.path(DRIVER);
            if (!driver.isTextual()) {
                return false;
            }

            JsonNode rteHost = node.path(RTE_HOST);
            if (!rteHost.isTextual()) {
                return false;
            }

            try {
                Ip4Address.valueOf(rteHost.asText());
            }
            catch (IllegalArgumentException e) {
                return false;
            }

            JsonNode rtePort = node.path(RTE_PORT);
            if (!rtePort.isTextual() || (rteHost.intValue() >= 1 && rteHost.intValue() <= 65535)) {
                return false;
            }
        }

        return true;
    }

    public Set<NfpNicDevice> getNfpNicDevices() throws ConfigException {
        Set<NfpNicDevice> nfpNicDevices = Sets.newHashSet();
        try {
            for (JsonNode node : array) {
                String dpId = node.path(DP_ID).asText(null);
                String driver = node.path(DRIVER).asText(null);
                String rteHost = node.path(RTE_HOST).asText(null);
                String rtePort = node.path(RTE_PORT).asText(null);
                nfpNicDevices.add(new NfpNicDevice(dpId, driver, rteHost, rtePort));
            }
        }
        catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return nfpNicDevices;
    }
}
