package Server.net;

import Common.Model.net.SocketRequest;
import Common.Model.net.SocketResponse;

public interface SocketActionHandler {
    SocketResponse handle(SocketRequest request);
}

