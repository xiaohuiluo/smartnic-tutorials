package cn.pcl.firewall;

import cn.pcl.firewall.rtecli.RteCliResponse;

public interface FirewallService {
    RteCliResponse addFlowRule(String rteHost, String rtePort);
}
