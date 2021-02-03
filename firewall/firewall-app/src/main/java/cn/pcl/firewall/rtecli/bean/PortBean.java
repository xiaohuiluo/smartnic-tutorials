package cn.pcl.firewall.rtecli.bean;

public class PortBean {
    private int id;
    private String info;
    private String token;

    public PortBean() {
    }

    public PortBean(int id, String info, String token) {
        this.id = id;
        this.info = info;
        this.token = token;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "PortBean{" +
                "id=" + id +
                ", info='" + info + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
