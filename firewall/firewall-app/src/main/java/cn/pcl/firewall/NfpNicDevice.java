package cn.pcl.firewall;

import com.google.common.collect.Lists;

import java.util.List;

public class NfpNicDevice {
    private String dpId;

    private String driver;

    private String rteHost;

    private String rtePort;

    private List<NfpNicPort> portList;

    public NfpNicDevice(String dpId, String driver, String rteHost, String rtePort) {
        this.dpId = dpId;
        this.driver = driver;
        this.rteHost = rteHost;
        this.rtePort = rtePort;
        this.portList = Lists.newArrayList();
    }

    public String getDpId() {
        return dpId;
    }

    public void setDpId(String dpId) {
        this.dpId = dpId;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getRteHost() {
        return rteHost;
    }

    public void setRteHost(String rteHost) {
        this.rteHost = rteHost;
    }

    public String getRtePort() {
        return rtePort;
    }

    public void setRtePort(String rtePort) {
        this.rtePort = rtePort;
    }

    public List<NfpNicPort> getPortList() {
        return portList;
    }

    public void setPortList(List<NfpNicPort> portList) {
        this.portList = portList;
    }

    @Override
    public String toString() {
        return "NfpNicDevice{" +
                "dpId='" + dpId + '\'' +
                ", driver='" + driver + '\'' +
                ", rteHost='" + rteHost + '\'' +
                ", rtePort='" + rtePort + '\'' +
                ", portList=" + portList +
                '}';
    }

    public static class NfpNicPort {
        private String id;

        private String info;

        private String token;
    }
}
