package cn.pcl.firewall.rtecli;

public class RteCliResponse {
    private boolean result;

    private String message;

    public RteCliResponse(boolean result) {
        this.result = result;
    }

    public RteCliResponse(boolean result, String message) {
        this.result = result;
        this.message = message;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
