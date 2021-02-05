package cn.pcl.firewall.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

@Service
@Command(scope = "onos", name = "acl", description = "Acl command")
public class AclCommand extends AbstractShellCommand
{
    @Override
    protected void doExecute() throws Exception {

    }
}
