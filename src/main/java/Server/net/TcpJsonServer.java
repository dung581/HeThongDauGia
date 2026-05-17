package Server.net;

import Common.Model.net.SocketRequest;
import Common.Model.net.SocketResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpJsonServer {
    private final int port;
    private final Gson gson = new Gson();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map<String, SocketActionHandler> handlers = new HashMap<>();

    public TcpJsonServer(int port) {
        this.port = port;
        AuthSocketHandler authHandler = new AuthSocketHandler();
        handlers.put("auth.login", authHandler);
        handlers.put("auth.register", authHandler);
    }

    public void start() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] TCP JSON server started at port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                pool.submit(() -> serveClient(client));
            }
        }
    }

    private void serveClient(Socket client) {
        try (Socket socket = client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                SocketResponse response;
                try {
                    SocketRequest request = gson.fromJson(line, SocketRequest.class);
                    if (request == null || request.getAction() == null) {
                        response = SocketResponse.fail("Invalid request");
                    } else {
                        SocketActionHandler handler = handlers.get(request.getAction());
                        response = handler == null
                                ? SocketResponse.fail("Unsupported action: " + request.getAction())
                                : handler.handle(request);
                    }
                } catch (Exception e) {
                    response = SocketResponse.fail("Bad JSON: " + e.getMessage());
                }

                writer.write(gson.toJson(response));
                writer.newLine();
                writer.flush();
            }
        } catch (Exception ignored) {
        }
    }
}

