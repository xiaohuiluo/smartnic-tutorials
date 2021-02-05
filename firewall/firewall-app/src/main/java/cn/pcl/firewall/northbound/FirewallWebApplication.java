package cn.pcl.firewall.northbound;

import org.onlab.rest.AbstractWebApplication;

import java.util.Set;

public class FirewallWebApplication extends AbstractWebApplication {
    @Override
    public Set<Class<?>> getClasses() {
        return getClasses(FirewallWebResource.class);
    }
}
