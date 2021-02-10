package cn.pcl.firewall;

import cn.pcl.firewall.common.AclConfig;
import cn.pcl.firewall.common.CmdResult;
import cn.pcl.firewall.common.FwdConfig;

import java.util.Collection;

public interface FirewallService {

    Collection<AclConfig> getAclConfigs(String deviceId);

    CmdResult addAclFlowRule(String deviceId, String action, String ingressPort, String ethType, String srcMac, String dstMac,
                             String protocol, String srcIp, String dstIp, String srcPort, String dstPort);

    CmdResult deleteAclFlowRule(String deviceId, String id);

    Collection<FwdConfig> getFwdConfigs(String deviceId);

    CmdResult addFwdFlowRule(String deviceId, String ingressPort, String dstMac, String action, String output);

    CmdResult deleteFwdFlowRule(String deviceId, String id);
}
