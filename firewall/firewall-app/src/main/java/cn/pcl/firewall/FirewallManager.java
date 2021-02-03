/*
 * Copyright 2021-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.pcl.firewall;

import cn.pcl.firewall.config.NfpNicCfg;
import cn.pcl.firewall.rtecli.NfpRteCliController;
import cn.pcl.firewall.rtecli.bean.PortBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.PortDescriptionsConfig;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;

import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigException;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.onosproject.net.MastershipRole.MASTER;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class FirewallManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_NAME = "cn.pcl.firewall";

    public static final String NFP_NICS = "nfp_nics";

    public static final String DRIVER = "driver";

    public static final String SCHEME = "rtecli";

    public static final String PROVIDER_ID = "cn.pcl.provider.nfp.nic";

    public static final String NETRONOME = "Netronome";

    public static final String NFP_HW_VERSION = "NFP4001";

    public static final String NFP_SW_VERSION = "NFP-SDK-6.1.0.1";

    public static final String UNKNOWN = "UNKNOWN";

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipAdminService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService networkConfigService;

    private final ConfigFactory<ApplicationId, NfpNicCfg> nfpNicConfigFactory =
            new ConfigFactory<ApplicationId, NfpNicCfg>(
                    SubjectFactories.APP_SUBJECT_FACTORY,
                    NfpNicCfg.class, NFP_NICS, true) {
                @Override
                public NfpNicCfg createConfig() {
                    return new NfpNicCfg();
                }
            };


    private final InternalConfigListener cfgListener = new InternalConfigListener(this);

    private DeviceProviderService deviceProviderService;

    private DeviceProvider deviceProvider = new InternalDeviceProvider();

    private Map<String, NfpNicDevice> nfpNicMap = new ConcurrentHashMap<String, NfpNicDevice>();

    private AtomicLong chassisNumber = new AtomicLong(0);

    private NfpRteCliController rteCliController = new NfpRteCliController();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        deviceProviderService = deviceProviderRegistry.register(deviceProvider);
        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(nfpNicConfigFactory);

        log.info("Started app {}", APP_NAME);
    }

    @Deactivate
    protected void deactivate() {
        deviceProviderRegistry.unregister(deviceProvider);
        deviceProviderService = null;
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(nfpNicConfigFactory);
        log.info("Stopped app ", APP_NAME);
    }

    private class InternalConfigListener implements NetworkConfigListener {

        private static final String NUMBER = "number";
        private static final String NAME = "name";
        private static final String ENABLED = "enabled";
        private static final String REMOVED = "removed";
        private static final String TYPE = "type";
        private static final String SPEED = "speed";
        private static final String ANNOTATIONS = "annotations";

        FirewallManager firewallManager;

        public InternalConfigListener(FirewallManager firewallManager) {
            this.firewallManager = firewallManager;
        }

        @Override
        public void event(NetworkConfigEvent event) {
            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED || event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                    event.configClass().equals(NfpNicCfg.class)) {
                NfpNicCfg nfpNicCfg = cfgService.getConfig(appId, NfpNicCfg.class);
                try {
                    Set<NfpNicDevice> nfpNicDeviceSet = nfpNicCfg.getNfpNicDevices();

                    nfpNicDeviceSet.forEach(nfpNic -> {
                        if (!nfpNic.getDpId().isEmpty() && !nfpNic.getRteHost().isEmpty() && !nfpNic.getRtePort().isEmpty() && !nfpNic.getDriver().isEmpty()) {
                            nfpNicMap.put(nfpNic.getDpId(), nfpNic);

                            // add device
                            DeviceId deviceId = DeviceId.deviceId(nfpNic.getDpId());
                            HashMap<String,String> annoMap = Maps.newHashMap();
                            annoMap.put(DRIVER, nfpNic.getDriver());
                            SparseAnnotations annotations = DefaultAnnotations.builder()
                                    .putAll(annoMap)
                                    .build();

                            DeviceDescription description =
                                    new DefaultDeviceDescription(deviceId.uri(), Device.Type.SWITCH,
                                            NETRONOME, NFP_HW_VERSION, NFP_SW_VERSION, UNKNOWN,
                                            new ChassisId(chassisNumber.getAndIncrement()), true, annotations);

                            // update ports
                            String ports = rteCliController.getNicPorts(nfpNic.getRteHost(), nfpNic.getRtePort());
                            log.debug("get nfp nic ports={}", ports);
                            try {
                                List<PortBean> portList = OBJECT_MAPPER.readValue(ports, new TypeReference<List<PortBean>>(){});
                                if (!portList.isEmpty()) {
                                    ObjectNode portsJson = OBJECT_MAPPER.createObjectNode();
                                    for (PortBean bean : portList) {

                                        HashMap<String,String> portAnnoMap = Maps.newHashMap();
                                        portAnnoMap.put("info", bean.getInfo());
                                        portAnnoMap.put("token", bean.getToken());
                                        SparseAnnotations portAnnotations = DefaultAnnotations.builder()
                                                .putAll(portAnnoMap)
                                                .build();

                                        String portName = bean.getId() + "";
                                        JsonNode portJsonNode = OBJECT_MAPPER.createObjectNode()
                                                .put(NUMBER, bean.getId())
                                                .put(NAME, portName)
                                                .put(ENABLED, false)
                                                .put(REMOVED, false)
                                                .put(TYPE, Port.Type.COPPER.toString())
                                                .put(SPEED, getPortSpeed(bean))
                                                .put(ANNOTATIONS, portAnnotations.toString());

                                        portsJson.set(portName, portJsonNode);
                                    }

                                    networkConfigService.applyConfig(deviceId, PortDescriptionsConfig.class, portsJson);

                                    PortDescriptionsConfig portConfig = networkConfigService.getConfig(deviceId, PortDescriptionsConfig.class);
                                    log.debug("deviceId={} netcfg port config = {}", deviceId, portConfig.portDescriptions());
                                }
                            }
                            catch (IOException e) {
                                log.warn("Failed to get nic ports of deviceId={}, exception={}", deviceId, e);
                            }

                            deviceProviderService.deviceConnected(deviceId, description);
                            NodeId localNode = clusterService.getLocalNode().id();
                            mastershipService.setRole(localNode, deviceId, MASTER);

                            // design load
                            boolean designLoadResult = rteCliController.designLoad(nfpNic.getRteHost(), nfpNic.getRtePort(),
                                    nfpNic.getNffwPath(), nfpNic.getDesignPath(), nfpNic.getP4cfgPath());

                            if (!designLoadResult) {
                                log.error("Failed to design load for nic {}", nfpNic);
                            }

                            log.info("Success to design load to nic {}", nfpNic);

                        } else {
                            log.error("Failed to add device for nic {}", nfpNic);
                        }
                    });

                }
                catch (ConfigException e) {
                    log.error("Failed to netcfg = {} , exception = {}", nfpNicCfg, e);
                }

            }
        }

        private long getPortSpeed(PortBean bean) {
            if (bean.getInfo().contains("10G")) {
                return 10000;
            }
            else if(bean.getInfo().contains("25G")) {
                return 25000;
            }
            else if(bean.getInfo().contains("40G")) {
                return 40000;
            }

            return 1000;
        }
    }

    private class InternalDeviceProvider implements DeviceProvider {
        @Override
        public void triggerProbe(DeviceId deviceId) {

        }

        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {

        }

        @Override
        public boolean isReachable(DeviceId deviceId) {

//            Device device = deviceService.getDevice(deviceId);
//            if (device.is(DeviceHandshaker.class)) {
//                DeviceHandshaker handshaker = device.as(DeviceHandshaker.class);
//
//                log.info("reach with driver isReachable={}", handshaker.isReachable());
//                return handshaker.isReachable();
//            }
            NfpNicDevice nicDevice = nfpNicMap.get(deviceId.toString());
            if ( nicDevice != null && rteCliController.connectNic(nicDevice.getRteHost(), nicDevice.getRtePort())) {
                return true;
            }

            return false;
        }

        @Override
        public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
            NfpNicDevice nicDevice = nfpNicMap.get(deviceId.toString());



        }

        @Override
        public ProviderId id() {
            return new ProviderId(SCHEME, PROVIDER_ID);
        }
    }

}
