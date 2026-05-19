package Server;

import Common.DataBase.DbIndexBootstrap;
import Server.net.TcpJsonServer;

public class Launcher {
    public static void main(String[] args) {
        int port = 8888;
        try {
            DbIndexBootstrap.ensureIndexes();
            new TcpJsonServer(port).start();
        } catch (Exception e) {
            throw new RuntimeException("Không thể chạy server ở port " + port, e);
        }
    }
}