package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import java.util.*;

import static server.Repository.*;


public class ClientHandler {
    private ConsoleServer server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    List<String> blackList;

    public ClientHandler(ConsoleServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            // черный список хранится локально, но берется из БД
            if (getBlackList(this) == null){
                this.blackList = null;
            } else {
                this.blackList = Arrays.asList((getBlackList(this)));
            }

            new Thread(() -> {
                boolean isExit = false;

                try {
                    while (true) {

                        String str = in.readUTF();
                        if (str.startsWith("/auth")){
                            String[] tokens = str.split(" ");
                            String nick = Repository.getNicknameByLoginAndPassword(tokens[1], tokens[2]);
                            if (nick != null) {
                                if (!server.isNickBusy(nick)) {
                                    sendMsg("/auth-OK " + nick);
                                    setNickname(nick);
                                    server.subscribe(ClientHandler.this);
                                    break;
                                } else {
                                    sendMsg("The account is in use");
                                }
                            } else {
                                sendMsg("Invalid username/password");
                            }
                        }
                        // регистрация
                        if (str.startsWith("/signup ")) {
                            String[] tokens = str.split(" ");
                            int result = Repository.addUser(tokens[1], tokens[2], tokens[3]);
                            if (result > 0) {
                                sendMsg("Successful registration");
                            } else {
                                sendMsg("Registration failed");
                            }
                        }
                        // выход
                        if ("/bye".equals(str)) {
                            isExit = true;
                            break;
                        }
                    }

                    if (!isExit) {
                        while (true) {
                            String str = in.readUTF();
                            // для всех служебных команд и личных сообщений
                            if (str.startsWith("/") || str.startsWith("@")) {
                                if ("/bye".equalsIgnoreCase(str)){
                                    // для оповещения клиента, т.к. без сервера клиент работать не должен
                                    sendMsg("/serverClosed");
                                    log.info(String.format("Client [%s]: exited", socket.getInetAddress()));
                                    break;
                                }
                                // приватные сообщения
                                if (str.startsWith("@")) {
                                    String[] tokens = str.split(" ", 2);
                                    server.sendPrivateMsg(this, tokens[0].substring(1), tokens[1]);
                                }
                                // черный список для пользователя
                                if (str.startsWith("/blacklist ")) {
                                    String[] tokens = str.split(" ");
                                    addBlackList(this, tokens[1]);
                                    sendMsg("You added " + tokens[1] + " to blacklist");
                                    this.blackList = Arrays.asList((getBlackList(this)));
                                }

                                if (str.startsWith("/removeBlacklist ")) {
                                    String[] tokens = str.split(" ");
                                    removeFromBlacklist(this, tokens[1]);
                                    sendMsg("You remove " + tokens[1] + " from blacklist");
                                    this.blackList = Arrays.asList((getBlackList(this)));
                                }
                            } else {
                                server.broadcastMessage(this, nickname +": " + str);
                            }
                            log.info(String.format("Client [%s]: %s", socket.getInetAddress(), str));
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                    server.unsubscribe(this);
                }
            }).start();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }

    public String getNickname() {
        return nickname;
    }

    private void setNickname(String nick) {
        this.nickname = nick;
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkBlackList(String nickname) {
        if(blackList == null) {
            return false;
        } else {
            return blackList.contains(nickname);
        }
    }
}
