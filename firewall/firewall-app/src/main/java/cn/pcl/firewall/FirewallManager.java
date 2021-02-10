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

import cn.pcl.firewall.common.AclConfig;
import cn.pcl.firewall.common.CmdResult;
import cn.pcl.firewall.common.Constants;
import cn.pcl.firewall.common.FwdConfig;
import cn.pcl.firewall.common.IntPool;
import cn.pcl.firewall.common.P4Constants;
import cn.pcl.firewall.config.NfpNicCfg;
import cn.pcl.firewall.rtecli.Action;
import cn.pcl.firewall.rtecli.Match;
import cn.pcl.firewall.rtecli.NfpRteCliController;
import cn.pcl.firewall.rtecli.RteCliResponse;
import cn.pcl.firewall.rtecli.bean.PortBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.glassfish.jersey.internal.guava.Sets;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.ConfigException;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.PortDescriptionsConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
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
@Component(immediate = true, service = {FirewallService.class})
public class FirewallManager implements FirewallService {

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

    public static final String ACL_RULE_NAME_HEADER = "ACL-";

    public static final String FWD_RULE_NAME_HEADER = "FWD-";


    private static final String NUMBER = "number";
    private static final String NAME = "name";
    private static final String ENABLED = "enabled";
    private static final String REMOVED = "removed";
    private static final String TYPE = "type";
    private static final String SPEED = "speed";
    private static final String ANNOTATIONS = "annotations";

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

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

    private ConsistentMap<String, NfpNicDevice> nfpNicMap;

    private Map<String, IntPool> aclIdPoolMap = new ConcurrentHashMap<>();

    private ConsistentMap<String, List<AclConfig>> aclConfigMap;

    private Map<String, IntPool> fwdIdPoolMap = new ConcurrentHashMap<>();

    private ConsistentMap<String, List<FwdConfig>> fwdConfigMap;

    private AtomicLong chassisNumber = new AtomicLong(0);

    private NfpRteCliController rteCliController = new NfpRteCliController();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        deviceProviderService = deviceProviderRegistry.register(deviceProvider);
        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(nfpNicConfigFactory);

        nfpNicMap = storageService.<String, NfpNicDevice>consistentMapBuilder()
                .withName("nfp-nic-map")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .withSerializer(Serializer.using( new KryoNamespace.Builder()
                        .register(KryoNamespaces.API)
                        .register(NfpNicDevice.class)
                        .build()))
                .build();

        aclConfigMap = storageService.<String, List<AclConfig>>consistentMapBuilder()
                .withName("acl-config-map")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(AclConfig.class)
                                .build()))
                .build();

        // update acl id pool resource
        aclConfigMap.asJavaMap().forEach( (deviceId, acls) -> {
            IntPool idPool = new IntPool(1000, 5000);
            for (AclConfig acl : acls) {
                idPool.markStatus(aclRuleId(acl.getId()), true);
            }

            aclIdPoolMap.put(deviceId, idPool);
        });

        fwdConfigMap = storageService.<String, List<FwdConfig>>consistentMapBuilder()
                .withName("fwd-config-map")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(FwdConfig.class)
                                .build()))
                .build();

        fwdConfigMap.asJavaMap().forEach( (deviceId, fwds) -> {
            IntPool idPool = new IntPool(1000, 2000);
            for (FwdConfig fwd : fwds) {
                idPool.markStatus(fwdRuleId(fwd.getId()), true);
            }

            fwdIdPoolMap.put(deviceId, idPool);
        });

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

    @Override
    public Collection<AclConfig> getAclConfigs(String deviceId) {
        NfpNicDevice device = nfpNicMap.asJavaMap().get(deviceId);
        if (device == null) {
            return Sets.newHashSet();
        }

        List<AclConfig> aclConfigList = aclConfigMap.asJavaMap().get(deviceId);

        if (aclConfigList == null) {
            return Sets.newHashSet();
        }
        else {
            return aclConfigMap.asJavaMap().get(deviceId);
        }
    }

    @Override
    public CmdResult addAclFlowRule(String deviceId, String action, String ingressPort, String ethType, String srcMac, String dstMac,
                                    String protocol, String srcIp, String dstIp, String srcPort, String dstPort) {
        NfpNicDevice device = nfpNicMap.asJavaMap().get(deviceId);
        if (device == null) {
            return new CmdResult(false, "nfp nic device = " + deviceId + " is not exist");
        }

        // action
        Action aclAction;
        if ("allow".equalsIgnoreCase(action)) {
            aclAction = new Action(P4Constants.ACT_ALLOW);
        } else if ("deny".equalsIgnoreCase(action)) {
            aclAction = new Action(P4Constants.ACT_DENY);
        }
        else {
            return new CmdResult(false, "action = " + action + " is invalid");
        }

        // match
        List<Match> matchList = Lists.newArrayList();
        if (ingressPort != null && !ingressPort.isEmpty()) {
            Match match = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_INGRESS_PORT, ingressPort, P4Constants.MASK_16_BITS);
            matchList.add(match);
        }

        if (srcMac != null && !srcMac.isEmpty()) {
            String srcMacHex = getHexMac(srcMac);
            Match match = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_ETH_SRC, srcMacHex, P4Constants.MASK_48_BITS);
            matchList.add(match);
        }

        if (dstMac != null && !dstMac.isEmpty()) {
            String dstMacHex = getHexMac(dstMac);
            Match match = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_ETH_DST, dstMacHex, P4Constants.MASK_48_BITS);
            matchList.add(match);
        }

        if (ethType != null && !ethType.isEmpty()) {
            String ethTypeCode = getEthTypeCode(ethType);
            Match match = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_ETH_TYPE, ethTypeCode, P4Constants.MASK_16_BITS);
            matchList.add(match);

            if (Constants.ARP_CODE.equals(ethTypeCode)) {
                // ARP request/response acl
            }
            else if (Constants.IPV4_CODE.equals(ethTypeCode)) {

                if (srcIp != null && !srcIp.isEmpty()) {
                    String srcIpHex = getHexIpv4(srcIp);
                    Match srcIpMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_IP_SRC, srcIpHex, P4Constants.MASK_32_BITS);
                    matchList.add(srcIpMatch);
                }

                if (dstIp != null && !dstIp.isEmpty()) {
                    String dstIpHex = getHexIpv4(dstIp);
                    Match dstIpMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_IP_DST, dstIpHex, P4Constants.MASK_32_BITS);
                    matchList.add(dstIpMatch);
                }

                if (protocol != null && !protocol.isEmpty()) {
                    String protoCode = getIpProtoCode(protocol);
                    if (Constants.ICMP_CODE.equals(protoCode)) {
                        Match protoMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_IP_PROTO, protoCode, P4Constants.MASK_48_BITS);
                        matchList.add(protoMatch);
                    }
                    else if (Constants.TCP_CODE.equals(protoCode)) {
                        Match protoMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_IP_PROTO, protoCode, P4Constants.MASK_48_BITS);
                        matchList.add(protoMatch);

                        if (srcPort != null && !srcPort.isEmpty()) {
                            Match tpPortMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_TCP_SRC, srcPort, P4Constants.MASK_16_BITS);
                            matchList.add(tpPortMatch);
                        }

                        if (dstPort != null && !dstPort.isEmpty()) {
                            Match tpPortMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_TCP_DST, dstPort, P4Constants.MASK_16_BITS);
                            matchList.add(tpPortMatch);
                        }
                    }
                    else if (Constants.UDP_CODE.equals(protoCode)) {
                        Match protoMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_IP_PROTO, protoCode, P4Constants.MASK_48_BITS);
                        matchList.add(protoMatch);

                        if (srcPort != null && !srcPort.isEmpty()) {
                            Match tpPortMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_UDP_SRC, srcPort, P4Constants.MASK_16_BITS);
                            matchList.add(tpPortMatch);
                        }

                        if (dstPort != null && !dstPort.isEmpty()) {
                            Match tpPortMatch = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_UDP_DST, dstPort, P4Constants.MASK_16_BITS);
                            matchList.add(tpPortMatch);
                        }

                    }
                }
            }
        }

        if (matchList.isEmpty()) {
            log.error("Match can not be empty");
            return new CmdResult(false, "Error: match is empty");
        }

        IntPool aclIdPool;
        if (aclIdPoolMap.isEmpty()) {
            aclIdPool = new IntPool(1000, 5000);
            aclIdPoolMap.put(deviceId, aclIdPool);
        }
        else {
            aclIdPool = aclIdPoolMap.get(deviceId);
        }

        int aclId = aclIdPool.acquire();
        String ruleName = aclRuleName(aclId);
        RteCliResponse response = rteCliController.addFlowRule(device.getRteHost(), device.getRtePort(), P4Constants.TBL_ACL, ruleName, matchList, aclAction);
        if (response.isResult()) {
            AclConfig config = new AclConfig(ruleName, action.toLowerCase(), ingressPort, ethType, srcMac, dstMac, protocol, srcIp, dstIp, srcPort, dstPort);
            saveAclConfig(deviceId, config);
            log.info("Success to Add acl={} to deviceId={}", config, deviceId);
            return new CmdResult(true, "Success to add acl = " + config);
        }
        else {
            aclIdPool.release(aclId);
            log.error("Failed to add acl to deviceId={}", deviceId);
            return new CmdResult(false, response.getMessage());
        }
    }

    @Override
    public CmdResult deleteAclFlowRule(String deviceId, String aclId) {
        NfpNicDevice device = nfpNicMap.asJavaMap().get(deviceId);
        if (device == null) {
            return new CmdResult(false, "nfp nic device = " + deviceId + " is not exist");
        }

        List<AclConfig> aclConfigList = aclConfigMap.asJavaMap().get(deviceId);

        if (aclConfigList == null || aclConfigList.isEmpty()) {
            return new CmdResult(false, "No such acl id=" + aclId);
        }

        // find acl
        for (AclConfig config : aclConfigList) {
            if (config.getId().equals(aclId)) {
                RteCliResponse response = rteCliController.deleteFlowRule(device.getRteHost(), device.getRtePort(), P4Constants.TBL_ACL, aclId);
                if (response.isResult()) {
                    aclIdPoolMap.get(deviceId).release(aclRuleId(aclId));
                    aclConfigList.remove(config);
                    aclConfigMap.put(deviceId, aclConfigList);
                    return new CmdResult(true, "Success to delete acl=" + config);
                } else {
                    return new CmdResult(false, "Failed to delete acl=" + config + ", error=" + response.getMessage());
                }
            }
        }

        return new CmdResult(false, "No such acl id=" + aclId);
    }

    @Override
    public Collection<FwdConfig> getFwdConfigs(String deviceId) {
        NfpNicDevice device = nfpNicMap.asJavaMap().get(deviceId);
        if (device == null) {
            return Sets.newHashSet();
        }

        List<FwdConfig> fwdConfigList = fwdConfigMap.asJavaMap().get(deviceId);

        if (fwdConfigList == null) {
            return Sets.newHashSet();
        }
        else {
            return fwdConfigMap.asJavaMap().get(deviceId);
        }
    }

    @Override
    public CmdResult addFwdFlowRule(String deviceId, String ingressPort, String dstMac, String action, String output) {
        NfpNicDevice device = nfpNicMap.asJavaMap().get(deviceId);
        if (device == null) {
            return new CmdResult(false, "nfp nic device = " + deviceId + " is not exist");
        }

        // action
        Action fwdAction;

        Action.ActionParam actionParam = new Action.ActionParam(P4Constants.ACT_PARAM_PORT, output);
        List<Action.ActionParam> actionParamList = Lists.newArrayList(actionParam);
        if ("fwd".equalsIgnoreCase(action)) {
            fwdAction = new Action(P4Constants.ACT_FWD, actionParamList);
        } else if ("drop".equalsIgnoreCase(action)) {
            fwdAction = new Action(P4Constants.ACT_DROP);
        }
        else {
            return new CmdResult(false, "action = " + action + " is invalid");
        }

        // match
        List<Match> matchList = Lists.newArrayList();
        if (ingressPort != null && !ingressPort.isEmpty()) {
            Match match = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_INGRESS_PORT, ingressPort, P4Constants.MASK_16_BITS);
            matchList.add(match);
        }

        if (dstMac != null && !dstMac.isEmpty()) {
            String dstMacHex = getHexMac(dstMac);
            Match match = Match.MatchFactory.createTernaryMatch(P4Constants.MTH_ETH_DST, dstMacHex, P4Constants.MASK_48_BITS);
            matchList.add(match);
        }

        if (matchList.isEmpty()) {
            log.error("Match can not be empty");
            return new CmdResult(false, "Error: match is empty");
        }

        IntPool fwdIdPool;
        if (fwdIdPoolMap.isEmpty()) {
            fwdIdPool = new IntPool(1000, 2000);
            fwdIdPoolMap.put(deviceId, fwdIdPool);
        }
        else {
            fwdIdPool = fwdIdPoolMap.get(deviceId);
        }

        int fwdId = fwdIdPool.acquire();
        String ruleName = fwdRuleName(fwdId);
        RteCliResponse response = rteCliController.addFlowRule(device.getRteHost(), device.getRtePort(), P4Constants.TBL_FWD, ruleName, matchList, fwdAction);
        if (response.isResult()) {
            FwdConfig config = new FwdConfig(ruleName, ingressPort, dstMac, action, output);
            saveFwdConfig(deviceId, config);
            log.info("Success to Add fwd={} to deviceId={}", config, deviceId);
            return new CmdResult(true, "Success to add fwd = " + config);
        }
        else {
            fwdIdPool.release(fwdId);
            log.error("Failed to add fwd to deviceId={}", deviceId);
            return new CmdResult(false, response.getMessage());
        }
    }

    @Override
    public CmdResult deleteFwdFlowRule(String deviceId, String fwdId) {
        NfpNicDevice device = nfpNicMap.asJavaMap().get(deviceId);
        if (device == null) {
            return new CmdResult(false, "nfp nic device = " + deviceId + " is not exist");
        }

        List<FwdConfig> fwdConfigList = fwdConfigMap.asJavaMap().get(deviceId);

        if (fwdConfigList == null || fwdConfigList.isEmpty()) {
            return new CmdResult(false, "No such fwd id=" + fwdId);
        }

        // find fwd
        for (FwdConfig config : fwdConfigList) {
            if (config.getId().equals(fwdId)) {
                RteCliResponse response = rteCliController.deleteFlowRule(device.getRteHost(), device.getRtePort(), P4Constants.TBL_FWD, fwdId);
                if (response.isResult()) {
                    fwdIdPoolMap.get(deviceId).release(fwdRuleId(fwdId));
                    fwdConfigList.remove(config);
                    fwdConfigMap.put(deviceId, fwdConfigList);
                    return new CmdResult(true, "Success to delete fwd=" + config);
                } else {
                    return new CmdResult(false, "Failed to delete fwd=" + config + ", error=" + response.getMessage());
                }
            }
        }

        return new CmdResult(false, "No such fwd id=" + fwdId);
    }


    private String aclRuleName(int aclId) {
        return ACL_RULE_NAME_HEADER + aclId;
    }

    private int aclRuleId(String aclRuleName) {
        return Integer.parseInt(aclRuleName.replaceAll(ACL_RULE_NAME_HEADER, ""));
    }

    private String fwdRuleName(int fwdId) {
        return FWD_RULE_NAME_HEADER + fwdId;
    }

    private int fwdRuleId(String fwdRuleName) {
        return Integer.parseInt(fwdRuleName.replaceAll(FWD_RULE_NAME_HEADER, ""));
    }

    private void saveAclConfig(String deviceId, AclConfig aclConfig) {
        List<AclConfig> aclConfigList = aclConfigMap.asJavaMap().get(deviceId);
        if (aclConfigList == null || aclConfigList.isEmpty()) {
            aclConfigList = Lists.newArrayList(aclConfig);
            aclConfigMap.put(deviceId, aclConfigList);
        }
        else {
            aclConfigList.removeIf(config -> config.getId().equals(aclConfig.getId()));

            aclConfigList.add(aclConfig);
            aclConfigMap.put(deviceId, aclConfigList);
        }
    }

    private void saveFwdConfig(String deviceId, FwdConfig fwdConfig) {
        List<FwdConfig> fwdConfigList = fwdConfigMap.asJavaMap().get(deviceId);
        if (fwdConfigList == null || fwdConfigList.isEmpty()) {
            fwdConfigList = Lists.newArrayList(fwdConfig);
            fwdConfigMap.put(deviceId, fwdConfigList);
        }
        else {
            fwdConfigList.removeIf(config -> config.getId().equals(fwdConfig.getId()));

            fwdConfigList.add(fwdConfig);
            fwdConfigMap.put(deviceId, fwdConfigList);
        }
    }

    private String getEthTypeCode(String ethType) {
        switch (ethType) {
            case Constants.ARP : {
                return Constants.ARP_CODE;
            }
            case Constants.IPV4 : {
                return Constants.IPV4_CODE;
            }
            default:{
                throw new RuntimeException("Error: Invalid ethernet type");
            }
        }
    }

    private String getIpProtoCode(String prorocol) {
        switch (prorocol) {
            case Constants.ICMP : {
                return Constants.ICMP_CODE;
            }
            case Constants.TCP : {
                return Constants.TCP_CODE;
            }
            case Constants.UDP : {
                return Constants.UDP_CODE;
            }
            default:{
                throw new RuntimeException("Error: Invalid ip protocol");
            }
        }
    }

    private String getHexMac(String mac) {
        MacAddress macAddress = MacAddress.valueOf(mac);
        return String.format("0x%s", macAddress.toStringNoColon());
    }

    private String getHexIpv4(String ip) {
        Ip4Address ip4Address = Ip4Address.valueOf(ip);
        return String.format("0x%08x", ip4Address.toInt());
    }


    private void updatePortsDesc(NfpNicDevice nfpNic, DeviceId deviceId) {
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
                            .put(ENABLED, true)
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
                        if (!nfpNic.getDpId().isEmpty() && !nfpNic.getRteHost().isEmpty() && !nfpNic.getRtePort().isEmpty() && !nfpNic.getDriver().isEmpty()) {

                            if (nfpNicMap == null) {
                                return;
                            }
                            // add nfp device
                            NfpNicDevice nfpDeviceMap = nfpNicMap.asJavaMap().get(nfpNic.getDpId());
                            if (nfpDeviceMap == null) {
                                nfpNicMap.put(nfpNic.getDpId(), nfpNic);
                            }

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

                            deviceProviderService.deviceConnected(deviceId, description);
                            NodeId localNode = clusterService.getLocalNode().id();
                            mastershipService.setRole(localNode, deviceId, MASTER);

                            // design load
                            RteCliResponse response = rteCliController.designLoad(nfpNic.getRteHost(), nfpNic.getRtePort(),
                                    nfpNic.getNffwPath(), nfpNic.getDesignPath(), nfpNic.getP4cfgPath());

                            if (!response.isResult()) {
                                log.error("Failed to design load for nic {}, error is {}", nfpNic, response.getMessage());
                                return;
                            }
                            else {
                                updatePortsDesc(nfpNic, deviceId);
                                log.info("Success to design load to nic {}", nfpNic);
                            }

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

            // Now do not use driver to control just direct use rtecli command
//            Device device = deviceService.getDevice(deviceId);
//            if (device.is(DeviceHandshaker.class)) {
//                DeviceHandshaker handshaker = device.as(DeviceHandshaker.class);
//
//                log.info("reach with driver isReachable={}", handshaker.isReachable());
//                return handshaker.isReachable();
//            }
            NfpNicDevice device = nfpNicMap.asJavaMap().get(deviceId.toString());
            if (device != null) {
                if (rteCliController.designLoad(device.getRteHost(), device.getRtePort(), device.getNffwPath(), device.getDesignPath(), device.getP4cfgPath()).isResult()) {
                    updatePortsDesc(device, deviceId);
                    return true;
                }
            }

            log.error("Failed to connect with nic {}", deviceId);
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
