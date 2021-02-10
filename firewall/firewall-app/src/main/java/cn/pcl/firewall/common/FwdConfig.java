package cn.pcl.firewall.common;

public class FwdConfig {
    private final String id;

    private final String ingressPort;

    private final String dstMac;

    private final String action;

    private final String output;


    public FwdConfig(String id, String ingressPort, String dstMac, String action, String output) {
        this.id = id;
        this.ingressPort = ingressPort;
        this.dstMac = dstMac;
        this.action = action;
        this.output = output;
    }

    public String getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getOutput() {
        return output;
    }

    public String getIngressPort() {
        return ingressPort;
    }

    public String getDstMac() {
        return dstMac;
    }

    @Override
    public String toString() {
        return "FwdConfig{" +
                "id='" + id + '\'' +
                ", action='" + action + '\'' +
                ", output='" + output + '\'' +
                ", ingressPort='" + ingressPort + '\'' +
                ", dstMac='" + dstMac + '\'' +
                '}';
    }
}
