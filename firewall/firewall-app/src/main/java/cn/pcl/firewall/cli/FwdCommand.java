package cn.pcl.firewall.cli;

import cn.pcl.firewall.FirewallService;
import cn.pcl.firewall.common.CmdResult;
import cn.pcl.firewall.common.FwdConfig;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

@Service
@Command(scope = "onos", name = "fwd", description = "FWD command")
public class FwdCommand extends AbstractShellCommand
{
    @Argument(index = 0, name = "ops", description = "operation of acl (support: show/add/delete)",
            required = true, multiValued = false)
    String ops = null;

    @Argument(index = 1, name = "deviceId", description = "deviceId you want to apply",
            required = true, multiValued = false)
    String deviceId = null;

    @Argument(index = 2, name = "actionOrId", description = "action/id of fwd, action for add and id for delete (action support: fwd, id support: 1000~2000)",
            required = false, multiValued = false)
    String actionOrId = null;

    @Argument(index = 3, name = "output", description = "output port for action fwd",
            required = false, multiValued = false)
    String output = null;

    @Option(name = "-p", aliases = "--ingressPort", description = "Ingress port of package (support: 0~65535)",
            required = false, multiValued = false)
    String ingressPort = null;

    @Option(name = "-d", aliases = "--ethDst", description = "Ethernet dest MAC address of package (support mac format: 54:bf:64:a1:9f:8b)",
            required = false, multiValued = false)
    String dstMac = null;

    private static final String STRING_FORMAT = "%s";
    private static final String FWD_STRING_FORMAT = "| %s | %-11s | %-17s | %-8s | %-7s |";

    @Override
    protected void doExecute() throws Exception {
        switch (ops) {
            case "show" : {
                showFwdConfig();
                break;
            }
            case "add" : {
                addFwdConfig();
                break;
            }
            case "delete" : {
                deleteFwdConfig();
                break;
            }
            default: {
                log.error("error command parameters");
                break;
            }
        }

    }

    private void showFwdConfig() {
        FirewallService firewallService = AbstractShellCommand.get(FirewallService.class);
        print(STRING_FORMAT, "===================================================================");
        print(STRING_FORMAT, "|    ID    |                MATCH            |       ACTION       |");
        print(STRING_FORMAT, "===================================================================");
        print(STRING_FORMAT, "|    ID    | IngressPort |       DstMac      |  Action  | Output  |");
        print(STRING_FORMAT, "-------------------------------------------------------------------");
        Iterable<FwdConfig> fwdConfigs = firewallService.getFwdConfigs(deviceId);
        for (FwdConfig config : fwdConfigs) {
            print(FWD_STRING_FORMAT, config.getId(),
                    getString(config.getIngressPort()),
                    getString(config.getDstMac()),
                    config.getAction(),
                    getString(config.getOutput()));
        }
        print(STRING_FORMAT, "-------------------------------------------------------------------");

    }

    private void addFwdConfig() {
        if (!checkAddFwdArgs()) {
            print(STRING_FORMAT, "Argument error, actionOrId can not be none");
        }

        FirewallService firewallService = AbstractShellCommand.get(FirewallService.class);

        CmdResult cmdResult = firewallService.addFwdFlowRule(deviceId, ingressPort,  dstMac, actionOrId, output);
        print(STRING_FORMAT, cmdResult.getMsg());
    }

    private void deleteFwdConfig() {
        if (actionOrId == null || actionOrId.isEmpty()) {
            print(STRING_FORMAT, "Argument error, actionOrId can not be none");
        }
        FirewallService firewallService = AbstractShellCommand.get(FirewallService.class);
        CmdResult cmdResult = firewallService.deleteFwdFlowRule(deviceId, actionOrId);
        print(STRING_FORMAT, cmdResult.getMsg());
    }

    private String getString(String str) {
        if (str == null) {
            return "";
        }

        return str;
    }

    private boolean checkAddFwdArgs(){
        if (actionOrId == null || actionOrId.isEmpty()) {
            return false;
        }

        return true;
    }
}
