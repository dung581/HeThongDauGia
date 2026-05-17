package Common.Model.net;

import java.util.Map;

public class SocketRequest {
    private String action;
    private Map<String, Object> payload;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}

