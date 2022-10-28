package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static java.lang.Thread.sleep;

public class Controller implements Initializable {

    @FXML
    TextField enterText;
    @FXML
    Label userName;
    @FXML
    TextArea chatArea;
    @FXML
    HBox bottomPanel;
    @FXML
    HBox loginingPanel;
    @FXML
    HBox upperPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;

    @FXML
    ListView<String> clientList;

    static Socket socket;
    DataInputStream in;
    DataOutputStream out;

    static final String SERVER_ADDR = "localhost";
    static final int SERVER_PORT = 8990;

    private boolean isAuthorized;
    List<TextArea> textAreas;

    public void setAuthorized(boolean authorized) {
        this.isAuthorized = authorized;

        upperPanel.setVisible(!isAuthorized);
        upperPanel.setManaged(!isAuthorized);

        bottomPanel.setVisible(isAuthorized);
        bottomPanel.setManaged(isAuthorized);

        clientList.setVisible(isAuthorized);
        clientList.setManaged(isAuthorized);

        loginingPanel.setVisible(isAuthorized);
        loginingPanel.setManaged(isAuthorized);

    }

    @FXML
    void sendText() {
        if (!enterText.getText().equals("")) {
            try {
                out.writeUTF(enterText.getText());
                enterText.clear();
                enterText.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void connect() {
        try {
            socket = new Socket(SERVER_ADDR, SERVER_PORT);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/auth-OK")) {
                            String[] s = str.split(" ");
                            setAuthorized(true);
                            chatArea.clear();
                            Platform.runLater(() -> userName.setText(s[1]));
                            break;
                        } else {
                            for (TextArea ta : textAreas) {
                                ta.appendText(str + "\n");
                            }
                        }
                    }

                    try {
                        while (true) {
                            String str = in.readUTF();
                            if ("/serverClosed".equals(str)) {
                                break;
                            }
                            if (str.startsWith("/clientList ")) {
                                String[] tokens = str.split(" ");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientList.getItems().add(tokens[i]);
                                    }
                                });
                            } else {
                                chatArea.appendText(str + "\n");
                            }
                        }
                    } catch (SocketException e) {
                        System.out.println("Соединение прервано");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setAuthorized(false);
                }
            }).start();
            timeOut();
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Отказано в соединении\n");
        }
    }

    public void disconnect() {
        if (socket != null) {
            try {
                out.writeUTF("/bye");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void clearChat() {
        chatArea.clear();
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void selectClient(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            MiniStage ms = new MiniStage(clientList.getSelectionModel().getSelectedItem(), out, textAreas);
            ms.show();
        }
    }

    public void addToBlacklist() {
        if (clientList.getSelectionModel().getSelectedItem() != null) {
            try {
                out.writeUTF("/blacklist " + clientList.getSelectionModel().getSelectedItem());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeFromBlacklist() {
        if (clientList.getSelectionModel().getSelectedItem() != null) {
            try {
                out.writeUTF("/removeBlacklist " + clientList.getSelectionModel().getSelectedItem());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        textAreas = new ArrayList<>();
        textAreas.add(chatArea);
    }

    public void logUp(ActionEvent actionEvent) {
        RegistrationStage rs = new RegistrationStage(out);
        rs.show();
    }

    // отключение неавторизованных пользователей
    public void timeOut() {
        Thread timer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sleep(30000);
                    if (!isAuthorized) {
                        clearChat();
                        chatArea.appendText("Таймаут соединения\n");
                        disconnect();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
        timer.start();
    }
}

