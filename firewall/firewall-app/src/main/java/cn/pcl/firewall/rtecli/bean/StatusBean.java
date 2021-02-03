package cn.pcl.firewall.rtecli.bean;

public class StatusBean {
    private String frontend_build_date;

    private String frontend_source;

    private String frontend_version;

    private boolean is_loaded;

    private long uptime;

    private String uuid;

    public StatusBean() {
    }

    public StatusBean(String frontend_build_date, String frontend_source, String frontend_version, boolean is_loaded, long uptime, String uuid) {
        this.frontend_build_date = frontend_build_date;
        this.frontend_source = frontend_source;
        this.frontend_version = frontend_version;
        this.is_loaded = is_loaded;
        this.uptime = uptime;
        this.uuid = uuid;
    }

    public String getFrontend_build_date() {
        return frontend_build_date;
    }

    public void setFrontend_build_date(String frontend_build_date) {
        this.frontend_build_date = frontend_build_date;
    }

    public String getFrontend_source() {
        return frontend_source;
    }

    public void setFrontend_source(String frontend_source) {
        this.frontend_source = frontend_source;
    }

    public String getFrontend_version() {
        return frontend_version;
    }

    public void setFrontend_version(String frontend_version) {
        this.frontend_version = frontend_version;
    }

    public boolean isIs_loaded() {
        return is_loaded;
    }

    public void setIs_loaded(boolean is_loaded) {
        this.is_loaded = is_loaded;
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "StatusBean{" +
                "frontend_build_date='" + frontend_build_date + '\'' +
                ", frontend_source='" + frontend_source + '\'' +
                ", frontend_version='" + frontend_version + '\'' +
                ", is_loaded=" + is_loaded +
                ", uptime=" + uptime +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
