package cn.pcl.firewall.cli;

import cn.pcl.firewall.FirewallService;
import cn.pcl.firewall.common.AclConfig;
import cn.pcl.firewall.common.CmdResult;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

@Service
@Command(scope = "onos", name = "acl", description = "ACL command")
public class AclCommand extends AbstractShellCommand
{
    @Argument(index = 0, name = "ops", description = "operation of acl (support: show/add/delete)",
            required = true, multiValued = false)
    String ops = null;

    @Argument(index = 1, name = "deviceId", description = "deviceId you want to apply",
            required = true, multiValued = false)
    String deviceId = null;

    @Argument(index = 2, name = "actionOrId", description = "action/id of acl, action for add and id for delete (action support: deny/allow, id support: 1000~5000)",
            required = false, multiValued = false)
    String actionOrId = null;

    @Option(name = "-p", aliases = "--ingressPort", description = "Ingress port of package (support: 0~65535)",
            required = false, multiValued = false)
    String ingressPort = null;

    @Option(name = "-t", aliases = "--ethType", description = "Ethernet type of package (support: ipv4/ipv6/arp)",
            required = false, multiValued = false)
    String ethType = null;

    @Option(name = "-s", aliases = "--ethSrc", description = "Ethernet source MAC address of package (support mac format: 54:bf:64:a1:9f:8b)",
            required = false, multiValued = false)
    String srcMac = null;

    @Option(name = "-d", aliases = "--ethDst", description = "Ethernet dest MAC address of package (support mac format: 54:bf:64:a1:9f:8b)",
            required = false, multiValued = false)
    String dstMac = null;

//        @Option(name = "-v", aliases = "--vlan", description = "VLAN ID",
//                required = false, multiValued = false)
//        private String vlan = null;

    @Option(name = "--protocol", description = "protocol of package (support: icmp/tcp/udp)",
            required = false, multiValued = false)
    String protocol = null;

    @Option(name = "--ipSrc", description = "Source IP of package (support ipv4 address: 192.168.10.1)",
            required = false, multiValued = false)
    String srcIp = null;

    @Option(name = "--ipDst", description = "Dest IP of package (support ipv4 address: 192.168.10.2)",
            required = false, multiValued = false)
    String dstIp = null;

    @Option(name = "--tpSrc", description = "Source tcp/udp port of package (support: 1~65535)",
            required = false, multiValued = false)
    String srcPort = null;

    @Option(name = "--tpDst", description = "Dest tcp/udp port of package (support: 1~65535)",
            required = false, multiValued = false)
    String dstPort = null;

    private static final String STRING_FORMAT = "%s";
    private static final String ACT_STRING_FORMAT = "| %s | %-11s | %-17s | %-17s | %-7s | %-8s | %-15s | %-15s |  %-5s  |  %-5s  | %-6s |";

    @Override
    protected void doExecute() throws Exception {
        switch (ops) {
            case "show" : {
                showAclConfig();
                break;
            }
            case "add" : {
                addAclConfig();
                break;
            }
            case "delete" : {
                deleteAclConfig();
                break;
            }
            default: {
                log.error("error command parameters");
                break;
            }
        }

    }

    private void showAclConfig() {
        FirewallService firewallService = AbstractShellCommand.get(FirewallService.class);
        print(STRING_FORMAT, "========================================================================================================================================================");
        print(STRING_FORMAT, "|    ID    |                                                             MATCH                                                                | ACTION |");

        print(STRING_FORMAT, "========================================================================================================================================================");
        print(STRING_FORMAT, "|    ID    | IngressPort |       SrcMac      |       DstMac      | EthType | Protocol |      SrcIp      |       DstIp     | SrcPort | DstPort | Action |");
        print(STRING_FORMAT, "--------------------------------------------------------------------------------------------------------------------------------------------------------");
        Iterable<AclConfig> aclConfigs = firewallService.getAclConfigs(deviceId);
        for (AclConfig config : aclConfigs) {
            print(ACT_STRING_FORMAT, config.getId(),
                    getString(config.getIngressPort()),
                    getString(config.getSrcMac()),
                    getString(config.getDstMac()),
                    getString(config.getEthType()),
                    getString(config.getProtocol()),
                    getString(config.getSrcIp()),
                    getString(config.getDstIp()),
                    getString(config.getSrcPort()),
                    getString(config.getDstPort()),
                    config.getAction());
        }
        print(STRING_FORMAT, "--------------------------------------------------------------------------------------------------------------------------------------------------------");

    }

    private void addAclConfig() {
        if (!checkAddAclArgs()) {
            print(STRING_FORMAT, "Argument error, actionOrId can not be none");
        }

        FirewallService firewallService = AbstractShellCommand.get(FirewallService.class);

        CmdResult cmdResult = firewallService.addAclFlowRule(deviceId, actionOrId, ingressPort, ethType, srcMac, dstMac, protocol, srcIp, dstIp, srcPort, dstPort);
        print(STRING_FORMAT, cmdResult.getMsg());
    }

    private void deleteAclConfig() {
        if (actionOrId == null || actionOrId.isEmpty()) {
            print(STRING_FORMAT, "Argument error, actionOrId can not be none");
        }
        FirewallService firewallService = AbstractShellCommand.get(FirewallService.class);
        CmdResult cmdResult = firewallService.deleteAclFlowRule(deviceId, actionOrId);
        print(STRING_FORMAT, cmdResult.getMsg());
    }

    private String getString(String str) {
        if (str == null) {
            return "";
        }

        return str;
    }

    private boolean checkAddAclArgs(){
        if (actionOrId == null || actionOrId.isEmpty()) {
            return false;
        }

        return true;
    }
}
