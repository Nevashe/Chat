package server;


import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;


public class ConsoleServer {
    public static final Logger log = LogManager.getLogger("Server");

    private final int SERVER_PORT = 8990;

    private ServerSocket server;
    private Socket socket;

    private Vector<ClientHandler> users;

    public ConsoleServer() {
        users = new Vector<>();
        server = null;
        socket = null;
        try {
            Repository.connect();
            server = new ServerSocket(SERVER_PORT);
            log.info("Server started");

            while (true) {
                socket = server.accept();
                log.info(String.format("Client [%s] connected", socket.getInetAddress()));
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            log.error("Server can't start" + e.getMessage());
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                log.error("Socket can't close" + e.getMessage());
            }
            try {
                if (server != null) {
                    server.close();
                }
            } catch (IOException e) {
                log.error("Server can't close" + e.getMessage());
            }
            Repository.disconnect();
        }
    }

    public void broadcastMessage(ClientHandler from, String str) {
        for (ClientHandler c : users) {
            if (!c.checkBlackList(from.getNickname())) {
                c.sendMsg(str);
                Repository.addInBDMessages(c, str);
            }
        }
    }

    public void subscribe(ClientHandler client) {
        users.add(client);
        log.info(String.format("User [%s] connected", client.getNickname()));
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler client) {
        users.remove(client);
        log.info(String.format("User [%s] disconnected", client.getNickname()));
        broadcastClientsList();
    }

    public static void main(String[] args) {
        new ConsoleServer();
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler c : users) {
            if (c.getNickname().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public void sendPrivateMsg(ClientHandler nickFrom, String nickTo, String msg) {
        for (ClientHandler c : users) {
            if (c.getNickname().equals(nickTo) && !(c.checkBlackList(nickFrom.getNickname()))) {
                if (!nickFrom.getNickname().equals(nickTo)) {
                    String textMsg = nickFrom.getNickname() + ": [Send for " + nickTo + "] " + msg;
                    c.sendMsg(textMsg);
                    nickFrom.sendMsg(textMsg);
                }
            }
        }
    }

    private void broadcastClientsList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientList ");
        for (ClientHandler c : users) {
            sb.append(c.getNickname() + " ");
        }

        String out = sb.toString();
        for (ClientHandler c : users) {
            c.sendMsg(out);
        }
    }
}