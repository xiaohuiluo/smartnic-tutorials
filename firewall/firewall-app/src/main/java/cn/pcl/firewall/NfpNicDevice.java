package cn.pcl.firewall;

public class NfpNicDevice {
    private String dpId;

    private String driver;

    private String rteHost;

    private String rtePort;

    private String nffwPath;

    private String designPath;

    private String p4cfgPath;

    public NfpNicDevice(String dpId, String driver, String rteHost, String rtePort, String nffwPath, String designPath, String p4cfgPath) {
        this.dpId = dpId;
        this.driver = driver;
        this.rteHost = rteHost;
        this.rtePort = rtePort;
        this.nffwPath = nffwPath;
        this.designPath = designPath;
        this.p4cfgPath = p4cfgPath;
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

    public String getNffwPath() {
        return nffwPath;
    }

    public void setNffwPath(String nffwPath) {
        this.nffwPath = nffwPath;
    }

    public String getDesignPath() {
        return designPath;
    }

    public void setDesignPath(String designPath) {
        this.designPath = designPath;
    }

    public String getP4cfgPath() {
        return p4cfgPath;
    }

    public void setP4cfgPath(String p4cfgPath) {
        this.p4cfgPath = p4cfgPath;
    }

    @Override
    public String toString() {
        return "NfpNicDevice{" +
                "dpId='" + dpId + '\'' +
                ", driver='" + driver + '\'' +
                ", rteHost='" + rteHost + '\'' +
                ", rtePort='" + rtePort + '\'' +
                ", nffwPath='" + nffwPath + '\'' +
                ", designPath='" + designPath + '\'' +
                ", p4cfgPath='" + p4cfgPath + '\'' +
                '}';
    }
}
