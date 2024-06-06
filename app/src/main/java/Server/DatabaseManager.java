package Server;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import Objects.Contact;
import Objects.Message;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DatabaseManager {
    private  final String DATABASE_URL = "jdbc:mysql://34.175.206.223:3306/chat_database";
    private  final String USERNAME = "remote_user";
    private  final String PASSWORD = "password";
    private ExecutorService executorService = Executors.newFixedThreadPool(4);


    // MÉTODO PARA CERRAR LAS CONSULTAS CUANDO NO SE NECESITEN
    public void shutdown() {
        executorService.shutdown();
    }



    // MÉTODO PARA REGISTRAR UN USUARIO EN LA BASE DE DATOS
    public Future<Boolean> registerUserAsync(InputStream profileImageId, String password, String mail, String name, String telephoneNumber) {
        return executorService.submit(() -> registerUser(profileImageId, password, mail, name, telephoneNumber));
    }
    public boolean registerUser( InputStream profileImageId, String password, String mail, String name, String telephoneNumber) {
        boolean connect;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD)) {
            String sql = "INSERT INTO user (profile_image, password, mail, name, telephone_number) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                if (profileImageId == null) {
                    connect = false;
                    return connect;
                }

                statement.setBlob(1, profileImageId);
                statement.setString(2, password);
                statement.setString(3, mail);
                statement.setString(4, name);
                statement.setString(5, telephoneNumber);

                statement.executeUpdate();
                connect = true;
            }
        } catch (SQLException e) {
            connect = false;
            e.printStackTrace();
        }
        return connect;
    }
    // MÉTODO PARA COMPROBAR SI UN USUARIO EXISTE POR SU NOMBRE EN LA BASE DE DATOS
    public Future<Boolean> checkUsernameAsync(String username) {
        return executorService.submit(() -> checkUsername(username));
    }
    public boolean checkUsername(String username) {
        String query = "SELECT COUNT(*) FROM user WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count < 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    // MÉTODO PARA COMPROBAR SI UN USUARIO EXISTE POR SU CORREO EN LA BASE DE DATOS
    public Future<Boolean> checkMailAsync(String mail) {
        return executorService.submit(() -> checkMail(mail));
    }

    public boolean checkMail(String mail) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE mail = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, mail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count < 1;
                }
            }
        }
        return false;
    }
    // MÉTODO PARA COMPROBAR SI UN USUARIO EXISTE POR SU TELÉFONO EN LA BASE DE DATOS
    public Future<Boolean> checkTelephoneAsync(String telephone) {
        return executorService.submit(() -> checkTelephone(telephone));
    }
    public  boolean checkTelephone(String telephone) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE telephone_number = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, telephone);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count < 1;
                }
            }
        }
        return false;
    }
    // MÉTODO PARA COMPROBAR LAS CREDENCIALES DE INICIO DE SESIÓN
    public Future<Boolean> checkCredentialsAsync(String emailOrPhoneNumberOrUsername, String password) {
        return executorService.submit(() -> checkCredentials(emailOrPhoneNumberOrUsername,password));
    }

    public boolean checkCredentials(String emailOrPhoneNumberOrUsername, String password) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE (mail = ? OR telephone_number = ? OR name = ?) AND password = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, emailOrPhoneNumberOrUsername);
            statement.setString(2, emailOrPhoneNumberOrUsername);
            statement.setString(3, emailOrPhoneNumberOrUsername);
            statement.setString(4, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }
    // MÉTODO PARA DEVOLVER UN NOMBRE SEGUN SU NOMBRE, CORREO O TELEFONO Y SU CONTRASEÑA
    public Future<String> returnUsernameAsync(String emailOrPhoneNumberOrUsername, String password) {
        return executorService.submit(() -> returnUsername(emailOrPhoneNumberOrUsername,password));
    }

    public String returnUsername(String emailOrPhoneNumberOrUsername, String password) throws SQLException {
        String query = "SELECT name FROM user WHERE (mail = ? OR telephone_number = ? OR name = ?) AND password = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, emailOrPhoneNumberOrUsername);
            statement.setString(2, emailOrPhoneNumberOrUsername);
            statement.setString(3, emailOrPhoneNumberOrUsername);
            statement.setString(4, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return "";
    }
    // COMPROBAR QUE EXISTA UN USUARIO Y SACAR SU ID
    public Future<Integer> checkUserExistsAsync(String user) {
        return executorService.submit(() -> checkUserExists(user));
    }
    public int checkUserExists(String user) throws SQLException {
        int userId = -1;
        String query = "SELECT id FROM user WHERE name=? OR telephone_number=? OR mail=?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, user);
            statement.setString(2, user);
            statement.setString(3, user);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    userId = resultSet.getInt("id");
                }
            }
        }
        return userId;
    }

    // CREAR UN GRUPO Y AÑADIR USUARIOS A ESE GRUPO
    public Future<Boolean> createGroupAsync(String nameGroup, int idGroup, int idUser) {
        return executorService.submit(() -> createGroup(nameGroup,idGroup,idUser));
    }
    public boolean createGroup(String nameGroup, int idGroup, int idUser) throws SQLException {
        String query = "INSERT INTO groups (id_group, id_user, name) VALUES (?,?,?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, idGroup);
            preparedStatement.setInt(2, idUser);
            preparedStatement.setString(3, nameGroup);
            int rowsInserted = preparedStatement.executeUpdate();
            return rowsInserted > 0;
        }
    }
    // SACAR EL NOMBRE DE UN GRUPO
    public Future<String> getNameGroupAsync(int idGroup) {
        return executorService.submit(() -> getNameGroup(idGroup));
    }
    public String getNameGroup(int idGroup) throws SQLException {
        String query = "SELECT name FROM groups WHERE id_group = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, idGroup);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
            return "";
        }
    }
    // SACAR LA ID DE UN GRUPO
    public Future<Integer> getIdGroupAsync(String name) {
        return executorService.submit(() -> getIdGroup(name));
    }
    public int getIdGroup(String name) throws SQLException {
        String query = "SELECT id_group FROM groups WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, name);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
            return -1;
        }
    }
    // SACAR LA ID DE UN GRUPO
    public Future<Boolean> checkNameGroupAsync(String name) {
        return executorService.submit(() -> checkNameGroup(name));
    }
    public boolean checkNameGroup(String name){
        String query = "SELECT name FROM groups WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, name);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    // SACAR EL ÚLTIMO GRUPO PARA CREAR UNA ID NUEVA AL PRÓXIMO
    public Future<Integer> checkGroupsAsync() {
        return executorService.submit(() -> checkGroups());
    }
    public int checkGroups() throws SQLException {
        int groups = 0;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM groups ORDER BY id DESC");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                groups = resultSet.getInt(1);
            }
        }
        catch (SQLException e){
            groups = -1;
            return groups;
        }
        return groups;
    }
    // COMPROBAR LOS GRUPOS DE UN USUARIO
    public Future<ArrayList<Integer>> checkUserGroupsAsync(int idUser) {
        return executorService.submit(() -> checkUserGroups(idUser));
    }

    public ArrayList<Integer> checkUserGroups(int idUser) throws SQLException {
        ArrayList<Integer> groups = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id_group FROM groups WHERE id_user = ?")) {
            statement.setInt(1, idUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    groups.add(resultSet.getInt(1));
                }
            }
        }
        return groups;
    }
    // COMPROBAR LOS USUARIOS DE UN GRUPO
    public Future<Integer> checkUsersOnGroupsAsync(String name) {
        return executorService.submit(() -> checkUsersOnGroups(name));
    }

    public Integer checkUsersOnGroups(String name) throws SQLException {
        int numberOfUsers = 0;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id_user FROM groups WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    numberOfUsers = 1;
                    return numberOfUsers;
                }
            }
        }
        return numberOfUsers;
    }
    // ELIMINAR LOS CHATS DE UN GRUPO
    public void deleteChatsofGroupAsync(int groupId) {
        executorService.submit(() -> {deleteChatsofGroup(groupId);});
    }

    public void deleteChatsofGroup(int groupId) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM chat_group WHERE id_group = ?")) {

            statement.setInt(1, groupId);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    // ELIMINAR A UN USUARIO DE UN GRUPO
    public Future<Boolean> deleteFromGroupAsync(int idUser, int idGroup) {
        return executorService.submit(() -> deleteFromGroup(idUser, idGroup));
    }
    public boolean deleteFromGroup(int idUser, int idGroup) {
        String sql = "DELETE FROM groups WHERE (id_user = ? AND id_group = ?)";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            pstmt.setInt(2, idGroup);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // COMPROBAR SI UN CONTACTO FUE AGREGADO POR EL USUARIO
    public Future<Boolean> checkContactAddedAsync(int userId, int contactId) {
        return executorService.submit(() -> checkContactAdded(userId, contactId));
    }
    public boolean checkContactAdded(int userId, int contactId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM contact WHERE id_user_add_contact=? AND id_contact_added=?")) {
            statement.setInt(1, userId);
            statement.setInt(2, contactId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
    //COMPROBAR SI UN CONTACTO AGREGÓ AL USUARIO
    public Future<Boolean> checkContactAddedInverseAsync(int userId, int contactId) {
        return executorService.submit(() -> checkContactAddedInverse(userId, contactId));
    }

    public  boolean checkContactAddedInverse(int userId, int contactId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM contact WHERE id_contact_added=? AND id_user_add_contact=?")) {
            statement.setInt(1, userId);
            statement.setInt(2, contactId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
    //COMPROBAR CONTACTOS AGREGADOS POR EL USUARIO
    public Future<ArrayList<Integer>> checkContactsAsync(int userId) {
        return executorService.submit(() -> checkContacts(userId));
    }
    public  ArrayList<Integer> checkContacts(int userId) throws SQLException {
        ArrayList<Integer> contacts = new ArrayList<>();
        String query = "SELECT id_contact_added FROM contact WHERE id_user_add_contact = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    contacts.add(resultSet.getInt(1));
                }
            }
        }
        return contacts;
    }
    //COMPROBAR CONTACTOS QUE AGREGARON AL USUARIO
    public Future<ArrayList<Integer>> checkContactsInverseAsync(int userId) {
        return executorService.submit(() -> checkContactsInverse(userId));
    }
    public  ArrayList<Integer> checkContactsInverse(int userId) throws SQLException {
        ArrayList<Integer> contacts = new ArrayList<>();
        String query = "SELECT id_user_add_contact FROM contact WHERE id_contact_added = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    contacts.add(resultSet.getInt(1));
                }
            }
        }
        return contacts;
    }
    //CREAR UN NUEVO CONTACTO PARA EL USUARIO
    public Future<Boolean> addContactToUserAsync(int userId, int contactId) {
        return executorService.submit(() -> addContactToUser(userId, contactId));
    }
    public  boolean addContactToUser(int userId, int contactId) throws SQLException {
        String query = "INSERT INTO contact (id_user_add_contact, id_contact_added) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, contactId);
            int rowsInserted = statement.executeUpdate();
            return rowsInserted > 0;
        }
    }
    //DEVOLVER UNA ID SEGÚN SU CORREO, TELÉFONO O NOMBRE
    public Future<Integer> returnIdAsync(String name) {
        return executorService.submit(() -> returnId(name));
    }
    public  int returnId(String name) throws SQLException {
        String query = "SELECT id FROM user WHERE (mail = ? OR telephone_number = ? OR name = ?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, name);
            statement.setString(2, name);
            statement.setString(3, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }
    //DEVOLVER UNA NOMBRE SEGÚN SU ID
    public Future<String> returnUsernameByIdAsync(int id) {
        return executorService.submit(() -> returnUsernameById(id));
    }
    public  String returnUsernameById(int id) throws SQLException {
        String query = "SELECT name FROM user WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return "";
    }
    //DEVOLVER UN USUARIO SEGÚN SU ID
    public Future<Contact> returnUserByIdAsync(int id) {
        return executorService.submit(() -> returnUserById(id));
    }
    public Contact returnUserById(int id) throws SQLException {
        String query = "SELECT id, name, profile_image FROM user WHERE id = ?";
        Contact contact = null;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int userId = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    byte[] imageBytes = resultSet.getBytes("profile_image");
                    Bitmap profileImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    contact = new Contact(userId, name, profileImage);
                }
            }
        }
        return contact;
    }

    //SACAR EL ULTIMO MENSAJE ENTRE 2 USUARIOS
    public Future<String> getLastMessageAsync(int idUser, int id_contact) {
        return executorService.submit(() -> getLastMessage(idUser,id_contact));
    }
    public String getLastMessage(int idUser, int id_contact) throws SQLException {
        String query = "SELECT text_message FROM chat WHERE (user_id = ? AND contact_id = ?) OR (user_id = ? AND contact_id = ?) ORDER BY id DESC";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idUser);
            statement.setInt(2, id_contact);
            statement.setInt(3, id_contact);
            statement.setInt(4, idUser);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return "";
    }
    //SACAR EL ULTIMO MENSAJE DE UN GRUPO
    public Future<String> getLastMessageGroupAsync(int id_group) {
        return executorService.submit(() -> getLastMessageGroup(id_group));
    }
    public String getLastMessageGroup(int id_group) throws SQLException {
        String query = "SELECT text_message FROM chat_group WHERE id_group = ? ORDER BY id DESC";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, id_group);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return "";
    }
    //ELIMINAR UN CONTACTO DE AMBOS USUARIOS
    public Future<Boolean> deleteContactAsync(int idUser, int contactId) {
        return executorService.submit(() -> deleteContact(idUser, contactId));
    }
    public boolean deleteContact(int idUser, int contactId) {
        String sql = "DELETE FROM contact WHERE (id_user_add_contact = ? AND id_contact_added = ?) OR (id_user_add_contact = ? AND id_contact_added = ?)";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            pstmt.setInt(2, contactId);
            pstmt.setInt(3, contactId);
            pstmt.setInt(4, idUser);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    //ELIMINAR UN USUARIO
    public Future<Boolean> deleteUserAsync(int idUser) {
        return executorService.submit(() -> deleteUser(idUser));
    }
    public boolean deleteUser(int idUser) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    //ELIMINAR UN USUARIO DE LOS CONTACTOS DEL RESTO
    public Future<Boolean> deleteUserContactAsync(int idUser) {
        return executorService.submit(() -> deleteUserContact(idUser));
    }

    public boolean deleteUserContact(int idUser) {
        String sql = "DELETE FROM contact WHERE id_user_add_contact = ? OR id_contact_added = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            pstmt.setInt(2, idUser);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //SACAR LOS MENSAJES DE UN CHAT CON UN USUARIO
    public Future<ArrayList<Message>> getChatMessagesAsync(int user_id, int contact_id) {
        return executorService.submit(() -> getChatMessages(user_id, contact_id));
    }
    public ArrayList<Message> getChatMessages(int user_id, int contact_id) throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        String query = "SELECT user_id, contact_id, text_message FROM chat WHERE (user_id = ? AND contact_id = ?) OR (user_id = ? AND contact_id = ?)";

        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user_id);
            stmt.setInt(2, contact_id);
            stmt.setInt(3, contact_id);
            stmt.setInt(4, user_id);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                int contactId = rs.getInt("contact_id");
                String textMessage = rs.getString("text_message");
                messages.add(new Message(userId, contactId, textMessage));
            }
        }
        return messages;
    }
    //SACAR LOS MENSAJES DE UN CHAT CON UN GRUPO
    public Future<ArrayList<Message>> getChatMessagesGroupAsync(int user_id, int group_id) {
        return executorService.submit(() -> getChatMessagesGroup(user_id, group_id));
    }
    public ArrayList<Message> getChatMessagesGroup(int user_id, int group_id) throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        String query = "SELECT id_user, id_group, text_message FROM chat_group WHERE id_group = " +
                "(SELECT id_group FROM groups WHERE id_user = ? AND id_group = ?)";

        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user_id);
            stmt.setInt(2, group_id);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int userId = rs.getInt("id_user");
                int contactId = rs.getInt("id_group");
                String textMessage = rs.getString("text_message");
                messages.add(new Message(userId, contactId, textMessage));
            }
        }
        return messages;
    }
    //ENVIAR MENSAJE A UN USUARIO
    public Future<?> sendMessageAsync(int userId, int contactId, String textMessage) {
        return executorService.submit(() -> {
            try {
                sendMessage(userId, contactId,textMessage);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public synchronized void sendMessage(int userId, int contactId, String textMessage) throws SQLException {
        String query = "INSERT INTO chat (user_id, contact_id, text_message) VALUES (?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, contactId);
            statement.setString(3, textMessage);

            statement.executeUpdate();
        }
    }
    //ENVIAR MENSAJE A UN GRUPO
    public Future<?> sendMessageGroupAsync(int userId, int groupId, String textMessage) {
        return executorService.submit(() -> {
            try {
                sendMessageGroup(userId, groupId,textMessage);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public synchronized void sendMessageGroup(int userId, int groupId, String textMessage) throws SQLException {
        String query = "INSERT INTO chat_group (id_user, id_group, text_message) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, groupId);
            statement.setString(3, textMessage);
            statement.executeUpdate();
        }
    }
    //CONTAR MENSAJES DE GRUPOS Y CONTACTOS DE UN USUARIO
    public Future<Integer> countMessagesAsync(int user_id) {
        return executorService.submit(() -> countMessages(user_id));
    }
    public int countMessages(int userId) {
        int messageCount = 0;
        String query1 = "SELECT COUNT(*) FROM chat WHERE user_id = ? OR contact_id = ?";
        String query2 = "SELECT COUNT(*) FROM chat_group WHERE id_group IN (SELECT id_group FROM chat_group WHERE id_user = ?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement stmt1 = connection.prepareStatement(query1);
             PreparedStatement stmt2 = connection.prepareStatement(query2)) {
            stmt1.setInt(1, userId);
            stmt1.setInt(2, userId);
            ResultSet rs1 = stmt1.executeQuery();
            if (rs1.next()) {
                messageCount += rs1.getInt(1);
            }
            stmt2.setInt(1, userId);
            ResultSet rs2 = stmt2.executeQuery();
            if (rs2.next()) {
                messageCount += rs2.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messageCount;
    }
    //SACAR LA IMAGEN DE PERFIL DE UN USUARIO
    public Future<Bitmap> getProfileImageAsync(int user_id) {
        return executorService.submit(() -> getProfileImage(user_id));
    }
    public Bitmap getProfileImage(int id){
        String query = "SELECT profile_image FROM user WHERE id = ?";
        Bitmap image = null;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    byte[] imageBytes = resultSet.getBytes("profile_image");
                     image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return image;
    }
    //CAMBIAR LA IMAGEN DE PERFIL DE UN USUARIO
    public Future<Boolean> changeImageAsync(byte[] image, int idUser) {
        return executorService.submit(() -> changeImage(image, idUser));
    }
    public boolean changeImage(byte[] image, int idUser) {
        String query = "UPDATE user SET profile_image = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            // Convertir la imagen a un array de bytes y redimensionarla

            if (image == null) return false;

            // Establecer los bytes de la imagen en la consulta preparada
            stmt.setBytes(1, image);
            stmt.setInt(2, idUser);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}