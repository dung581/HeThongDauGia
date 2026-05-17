package Client.net;

import Common.Model.net.SocketRequest;
import Common.Model.net.SocketResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TcpJsonClient {
    private final String host;
    private final int port;
    private final Gson gson = new Gson();

    public TcpJsonClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public SocketResponse send(String action, Map<String, Object> payload) {
        try (Socket socket = new Socket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            SocketRequest request = new SocketRequest();
            request.setAction(action);
            request.setPayload(payload);

            writer.write(gson.toJson(request));
            writer.newLine();
            writer.flush();

            String responseJson = reader.readLine();
            if (responseJson == null) {
                return SocketResponse.fail("No response from server");
            }
            return gson.fromJson(responseJson, SocketResponse.class);
        } catch (Exception e) {
            return SocketResponse.fail("Connect fail: " + e.getMessage());
        }
    }
}

