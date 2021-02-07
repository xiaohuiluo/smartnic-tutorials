package cn.pcl.firewall.common;

public class AclConfig {
    private final String id;

    private final String action;

    private final String ingressPort;

    private final String ethType;

    private final String srcMac;

    private final String dstMac;

    private final String protocol;

    private final String srcIp;

    private final String dstIp;

    private final String srcPort;

    private final String dstPort;

    public AclConfig(String id, String action, String ingressPort, String ethType, String srcMac, String dstMac, String protocol, String srcIp, String dstIp, String srcPort, String dstPort) {
        this.id = id;
        this.action = action;
        this.ingressPort = ingressPort;
        this.ethType = ethType;
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.protocol = protocol;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }

    public String getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getIngressPort() {
        return ingressPort;
    }

    public String getEthType() {
        return ethType;
    }

    public String getSrcMac() {
        return srcMac;
    }

    public String getDstMac() {
        return dstMac;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public String getDstIp() {
        return dstIp;
    }

    public String getSrcPort() {
        return srcPort;
    }

    public String getDstPort() {
        return dstPort;
    }

    @Override
    public String toString() {
        return "AclConfig{" +
                "id='" + id + '\'' +
                ", action='" + action + '\'' +
                ", ingressPort='" + ingressPort + '\'' +
                ", ethType='" + ethType + '\'' +
                ", srcMac='" + srcMac + '\'' +
                ", dstMac='" + dstMac + '\'' +
                ", protocol='" + protocol + '\'' +
                ", srcIp='" + srcIp + '\'' +
                ", dstIp='" + dstIp + '\'' +
                ", srcPort='" + srcPort + '\'' +
                ", dstPort='" + dstPort + '\'' +
                '}';
    }
}
