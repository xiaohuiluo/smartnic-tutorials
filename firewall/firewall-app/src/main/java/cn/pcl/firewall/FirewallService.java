package cn.pcl.firewall;

import cn.pcl.firewall.common.AclConfig;
import cn.pcl.firewall.common.CmdResult;

import java.util.Collection;

public interface FirewallService {

    Collection<AclConfig> getAclConfigs(String deviceId);

    CmdResult addAclFlowRule(String deviceId, String action, String ingressPort, String ethType, String srcMac, String dstMac,
                             String protocol, String srcIp, String dstIp, String srcPort, String dstPort);

    CmdResult deleteAclFlowRule(String deviceId, String actionOrId);
}
