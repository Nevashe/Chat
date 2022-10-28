package server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.*;

public class Repository {
    private static Connection connection;
    private static Statement statement;
    public static final Logger log = LogManager.getLogger("Repo");

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:Server/src/main.db");
            statement = connection.createStatement();
            log.info("Connect to DB");

        } catch (ClassNotFoundException | SQLException e) {
            log.error("Connection DB error " + e.getMessage());
        }
    }

    public static int addUser(String login, String pass, String nickname) {
        try {
            String query = "INSERT INTO users (login, password, nickname) VALUES (?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setInt(2, pass.hashCode());
            ps.setString(3, nickname);
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.debug("User not added to DB " + e.getMessage());
        }
        return 0;
    }

    public static String getNicknameByLoginAndPassword(String login, String pass) {
        String query = String.format("select nickname, password from users where login='%s'", login);
        try {
            ResultSet rs = statement.executeQuery(query);
            int myHash = pass.hashCode();

            if (rs.next()) {
                String nick = rs.getString(1);
                int dbHash = rs.getInt(2);
                if (myHash == dbHash) {
                    return nick;
                }
            }
        } catch (SQLException e) {
            log.debug("User not retrieved from DB " + e.getMessage());
        }
        return null;
    }

    // получение черного списка из БД
    public static String[] getBlackList (ClientHandler c){
        String query = String.format("select blacklist from users where nickname='%s'", c.getNickname());
        String str = null;
        try {
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                str = rs.getString("blackList");
            }
        } catch (SQLException e) {
            log.debug("Blacklist not retrieved from DB " + e.getMessage());
        }
        if(str != null){
            return str.split(" ");
        }else{
            return null;
        }


    }

    // добавление в черный список
    public static void addBlackList(ClientHandler c, String nicknameInBlackList){
        String query = String.format("select blacklist from users where nickname='%s'", c.getNickname());
        String addInBlackList = null;
        try {
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                if(rs.getString("blackList") == null){
                    addInBlackList =nicknameInBlackList + " ";
                } else{
                    addInBlackList = rs.getString("blackList")  + nicknameInBlackList + " ";
                }
            }
            String active = String.format("UPDATE users SET blacklist ='%s' WHERE nickname ='%s'",addInBlackList, c.getNickname());
            statement.execute(active);
        } catch (SQLException e) {
            log.debug("User not added to Blacklist DB " + e.getMessage());
        }

    }

    public static void removeFromBlacklist(ClientHandler c, String nicknameInBlackList){
        String query = String.format("select blacklist from users where nickname='%s'", c.getNickname());
        String blackList;
        try {
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                if(rs.getString("blackList") != null){
                    blackList = rs.getString("blackList");
                    String newBlackList = blackList.replace(nicknameInBlackList + " ", "");

                    if(!blackList.equals(newBlackList)){
                        String active = String.format("UPDATE users SET blacklist ='%s' WHERE nickname ='%s'",newBlackList, c.getNickname());
                        statement.execute(active);
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("User not removed from Blacklist DB " + e.getMessage());
        }
    }
    // Добавление сообщений в базу данных
    public static void addInBDMessages(ClientHandler c, String msg) {
        try {
            String query = String.format("INSERT INTO messages (nickname, msg) VALUES ('%s', '%s')", c.getNickname(), msg);
            statement.execute(query);
        } catch (SQLException e) {
            log.debug("Msg not added to DB " + e.getMessage());
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            log.debug("Connection can't close " + e.getMessage());
        }
    }

}

