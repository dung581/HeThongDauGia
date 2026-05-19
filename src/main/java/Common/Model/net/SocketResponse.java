package Common.Model.net;

import java.util.Map;

public class SocketResponse {
    private boolean success;
    private String message;
    private Map<String, Object> data;

    public static SocketResponse ok(String message, Map<String, Object> data) {
        SocketResponse response = new SocketResponse();
        response.success = true;
        response.message = message;
        response.data = data;
        return response;
    }

    public static SocketResponse fail(String message) {
        SocketResponse response = new SocketResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getData() {
        return data;
    }
}

