package cn.pcl.firewall.common;

public class CmdResult {
    private boolean success;
    private String msg;

    public CmdResult(boolean success) {
        this.success = success;
        this.msg = "";
    }

    public CmdResult(boolean success, String msg) {
        this.success = success;
        this.msg = msg;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "CmdResult{" +
                "success=" + success +
                ", msg='" + msg + '\'' +
                '}';
    }
}
