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
import com.google.common.collect.Maps;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.provider.ProviderId;
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
import org.onosproject.net.device.DeviceProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
                        if (rteCliController.connectNic(nfpNic.getRteHost(), nfpNic.getRtePort())) {
                            if (!nfpNic.getDpId().isEmpty() && !nfpNic.getRteHost().isEmpty() && !nfpNic.getRtePort().isEmpty()) {
                                nfpNicMap.put(nfpNic.getDpId(), nfpNic);
                                DeviceId deviceId = DeviceId.deviceId(nfpNic.getDpId());
                                HashMap<String,String> annoMap = Maps.newHashMap();
                                annoMap.put(DRIVER, nfpNic.getDriver());
                                SparseAnnotations annotations = DefaultAnnotations.builder()
                                        .putAll(annoMap)
                                        .build();

                                DeviceDescription description =
                                        new DefaultDeviceDescription(deviceId.uri(), Device.Type.FIREWALL,
                                                NETRONOME, NFP_HW_VERSION, NFP_SW_VERSION, UNKNOWN,
                                                new ChassisId(chassisNumber.getAndIncrement()), true, annotations);
                                deviceProviderService.deviceConnected(deviceId, description);
                                NodeId localNode = clusterService.getLocalNode().id();
                                mastershipService.setRole(localNode, deviceId, MASTER);
                            }
                        } else {
                            log.error("Failed to connect {}", nfpNic);
                        }
                    });

                }
                catch (ConfigException e) {
                    log.error("Failed to netcfg = {} , exception = {}", nfpNicCfg, e);
                }

            }
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
            return false;
        }

        @Override
        public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {

        }

        @Override
        public ProviderId id() {
            return new ProviderId(SCHEME, PROVIDER_ID);
        }
    }

}
