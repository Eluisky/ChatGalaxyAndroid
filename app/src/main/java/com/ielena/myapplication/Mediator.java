package com.ielena.myapplication;


import static com.ielena.myapplication.Chat.idUser;
import static com.ielena.myapplication.Chat.username;
import static com.ielena.myapplication.Register.defaultImage;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Adapters.ChatAdapter;
import Adapters.ContactAdapter;
import Objects.Contact;
import Objects.Message;
import Server.User;
import Server.DatabaseManager;

public class Mediator {

    public static DatabaseManager databaseManager = new DatabaseManager();
    public static Context context;
    public static User user;
    static File rememberFile;
    public static final int PICK_IMAGE_REQUEST = 1;
    public static boolean userError = false;
    public static Thread threadReceiveMessages;
    private static ArrayList<Integer> contactsToBeAdded = new ArrayList<>();
    private static ArrayList<Integer> numberOfGroups;


    //RECORDAR USUARIO
    public static void remember(EditText emailOrPhoneNumberOrUsername, EditText loginPassword, CheckBox rememberMe, Context context) {
        //Inicializar el archivo
        rememberFile = new File(context.getFilesDir(), "remember.txt");
        //Recordar si el usuario pidió que se le recordara
        try {
            BufferedReader reader = new BufferedReader(new FileReader(rememberFile));
            //lee la primera linea
            emailOrPhoneNumberOrUsername.setText(reader.readLine());
            //lee la segunda linea
            loginPassword.setText(reader.readLine());
            reader.close();
            //Activamos de nuevo el checkbox
            rememberMe.setChecked(true);
        } catch (IOException e) {
            //System.err.println("El usuario no ha marcado la casilla");
        }
    }

    //COMPROBAR QUE TODOS LOS CAMPOS ESTÉN RELLENADOS
    public static boolean checkFields(String username, String mail, String telephone, String password) {
        if (!username.equals("") && !mail.equals("") && !telephone.equals("") && !password.equals("")) {

            return true;
        }
        return false;
    }

    //COMPROBAR EL FORMATO DE CORREO
    public static boolean checkMail(String mail) {
        // Patrón para validar correo electrónico
        String patronCorreo = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(patronCorreo);
        Matcher matcher = pattern.matcher(mail);
        return matcher.matches();
    }

    //COMPROBAR EL FORMATO DEL TELEFONO
    public static boolean checkTelephone(String telephone) {
        // Patrón para validar número de teléfono
        String telephonePattern = "^[0-9]{9}$";
        Pattern pattern = Pattern.compile(telephonePattern);
        Matcher matcher = pattern.matcher(telephone);
        return matcher.matches();
    }

    //COMPROBAR CONTRASEÑA
    public static boolean checkPassword(String password) {
        // Verificar si la contraseña tiene al menos ocho caracteres
        return password.length() >= 8;
    }

    //COMPROBAR QUE NO HAYA CAMPOS VACIOS EN EL LOGIN
    public static boolean checkLoginFields(String loginNameTelephoneMail, String password) {
        if (!loginNameTelephoneMail.equals("") && !password.equals("")) {
            return true;
        }
        return false;
    }

    //COMPROBAR INICIO DE SESIÓN
    public static Toast tryToLogin(EditText emailOrPhoneNumberOrUsername, EditText loginPassword, CheckBox rememberMe, Context context) {
        boolean login;
        Future<Boolean> loginFuture;
        Future<String> usernameFuture;
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        //Comprobar que no estén vacíos
        login = Mediator.checkLoginFields(emailOrPhoneNumberOrUsername.getText().toString(), loginPassword.getText().toString());

        if (!login) {
            //Aviso de que no ha rellenado todos los campos
            toast.setText("Rellene todos los campos para iniciar sesión");
            Login.logged = false;
            return toast;

        } else {
            //Comprobar que existan en la base de datos
            try {
                loginFuture = databaseManager.checkCredentialsAsync(emailOrPhoneNumberOrUsername.getText().toString(), loginPassword.getText().toString());
                login = loginFuture.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (!login) {
                toast.setText("No se ha encontrado una cuenta, compruebe las credenciales escritas");
                Login.logged = false;
                return toast;
            } else {
                if (rememberMe.isChecked()) {
                    //Si existe el archivo se borra
                    if (rememberFile.exists()) rememberFile.delete();
                    try {
                        rememberFile.createNewFile();
                        BufferedWriter write = new BufferedWriter(new FileWriter(rememberFile));
                        write.write(emailOrPhoneNumberOrUsername.getText().toString());
                        write.write("\n");
                        write.write(loginPassword.getText().toString());
                        write.close();
                    } catch (IOException e) {

                    }
                }
                //Si el usuario no marca la casilla, si el archivo existe lo borra
                else {
                    if (rememberFile.exists()) rememberFile.delete();
                }
                Login.logged = true;
                try {
                    usernameFuture = databaseManager.returnUsernameAsync(emailOrPhoneNumberOrUsername.getText().toString(), loginPassword.getText().toString());
                    Login.username = usernameFuture.get();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                toast.setText("¡Bienvenido/a " + Login.username + "!");
            }
        }
        return toast;
    }

    //LANZAR VENTANA DE REGISTRO
    public static Intent launchRegister(Context context) {
        // Crear el Intent
        Intent intent = new Intent(context, Register.class);
        // Lanzar la actividad
        return intent;
    }

    //METODO PARA REGISTRAR UN USUARIO
    public static Toast register(String username, String mail, String telephone, String password, Context context) throws SQLException, ExecutionException, InterruptedException {
        boolean fields;
        boolean checkMail;
        boolean checkTelephone;
        boolean checkPassword;
        boolean checkUsername;
        Future<Boolean> checkMailFuture;
        Future<Boolean> checkTelephoneFuture;
        Future<Boolean> checkUsernameFuture;

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setText("Error de conexion");

        fields = Mediator.checkFields(username, mail, telephone, password);

        if (fields) {
            //COMPROBAR CORREO
            checkMail = Mediator.checkMail(mail);

            if (!checkMail) {
                toast.setText("El correo electrónico no está escrito correctamente");
                return toast;
            } else {
                checkMailFuture = databaseManager.checkMailAsync(mail);
                checkMail = checkMailFuture.get();
                if (!checkMail) {
                    toast.setText("El correo electrónico ya existe en la base de datos con otra cuenta");
                    return toast;
                }
                //COMPROBAR TELÉFONO
                checkTelephone = Mediator.checkTelephone(telephone);
                if (!checkTelephone) {
                    toast.setText("El número de teléfono no está escrito correctamente");
                    return toast;
                } else {
                    checkTelephoneFuture = databaseManager.checkTelephoneAsync(telephone);
                    checkTelephone = checkTelephoneFuture.get();
                    if (!checkTelephone) {
                        toast.setText("El teléfono ya existe en la base de datos con otra cuenta");
                        return toast;
                    }
                }
                //COMPROBAR CONTRASEÑA
                checkPassword = Mediator.checkPassword(password);
                if (!checkPassword) {
                    toast.setText("La contraseña debe tener al menos 8 carácteres");
                    return toast;
                } else {
                    //COMPROBAR NOMBRE USUARIO
                    checkUsernameFuture = databaseManager.checkUsernameAsync(username);
                    checkUsername = checkUsernameFuture.get();
                    if (!checkUsername) {
                        toast.setText("El nombre de usuario ya existe en la base de datos");
                        return toast;
                    }

                    if (checkMail && checkTelephone && checkPassword && checkUsername) {

                        //Convertir la imagen
                        InputStream imageInputStream = getImageInputStream(context, defaultImage);
                        databaseManager.registerUserAsync(imageInputStream, password, mail, username, telephone);

                        Register.registred = true;
                        toast.setText("Registro correcto, ¡Gracias por registrarte!");
                        return toast;

                    }
                }
            }
        } else {
            toast.setText("Debes rellenar todos los campos para registrarte");
            return toast;
        }
        return toast;
    }

    // Método para obtener InputStream de un recurso drawable
    private static InputStream getImageInputStream(Context context, int imageResourceId) {
        Resources resources = context.getResources();
        Drawable drawable = resources.getDrawable(imageResourceId, null);
        Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public static Intent returnToLogin(Context context) {
        // Crear el Intent
        Intent intent = new Intent(context, Login.class);
        // Lanzar la actividad
        return intent;
    }

    public static Intent prepareUser(EditText emailOrPhoneNumberOrUsername, EditText loginPassword, Context context) throws SQLException, IOException {
        Future<String> usernameFuture;
        Future<Integer> idUserFuture;
        Future<Contact> userFuture;

        try {
            // La variable nombre la asignamos con el nombre del usuario que inicia sesión
            usernameFuture = databaseManager.returnUsernameAsync(emailOrPhoneNumberOrUsername.getText().toString(), loginPassword.getText().toString());
            username = usernameFuture.get();

            // Asignamos la imagen del usuario
            idUserFuture = databaseManager.returnIdAsync(username);
            Chat.idUser = idUserFuture.get();

            userFuture = databaseManager.returnUserByIdAsync(Chat.idUser);
            Contact user = userFuture.get();
            Chat.profileImage = new ImageView(context);
            Chat.profileImage.setImageBitmap(user.getProfile_image());

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        userThread(context);

        Intent intent = launchChat(context);
        //Devolver el intent
        return intent;

    }
    //HILO DEL USUARIO
    public static void userThread(Context context){
        threadReceiveMessages = new Thread(() -> {
            // Creamos el usuario para enviar mensajes
            User user = new User(Chat.idUser, context);
            Mediator.user = user;
            if (!userError) {
                // Hilo para actualizaciones en la vista
                new Thread(() -> {
                    while (user.waitForUpdate()) {
                    }
                }).start();
                // Continuar recibiendo mensajes mientras receiveMessage devuelva true
                while (user.receiveMessage()) {

                }
            }
        });
        threadReceiveMessages.start();
    }

    //LANZAR VENTANA DE CHATS
    public static Intent launchChat(Context context) {
        // Crear el Intent
        Intent intent = new Intent(context, Chat.class);
        intent.putExtra("username", Login.username);
        // Lanzar la actividad
        return intent;
    }
    //CARGAR IMAGEN USUARIO
    public static void userImage(String username){
        Future<Integer> idUserFuture;
        Future<Bitmap> imageFuture;
        try {
            idUserFuture = databaseManager.returnIdAsync(username);
            idUser = idUserFuture.get();
            //Conseguir la imagen de perfil del usuario
            imageFuture = databaseManager.getProfileImageAsync(idUser);
            Bitmap image = imageFuture.get();
            Chat.profileImage.setImageBitmap(image);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //CARGAR LOS CHATS DEL USUARIO CORRESPONDIENTE
    public static ArrayList<Contact> createChat(String username) {
        Future<Bitmap> imageFuture;
        Future<ArrayList<Integer>> contactsFuture;
        Future<String> contactNameFuture;
        ArrayList<Contact> contactList = new ArrayList<>();

        if (!contactList.isEmpty()) {
            contactList.clear();
        }
        //Guardar el id del usuario que inicia sesion
        try {
            //Imagen del usuario
            userImage(username);

            //Añade a los chats los usuarios que haya agregado y que le hayan agregado al usuario
            contactsFuture = databaseManager.checkContactsAsync(idUser);
            ArrayList<Integer> contacts = contactsFuture.get();
            contactsFuture = databaseManager.checkContactsInverseAsync(idUser);
            contacts.addAll(contactsFuture.get());

            for (int i = 0; i < contacts.size(); i++) {
                contactNameFuture = databaseManager.returnUsernameByIdAsync(contacts.get(i));
                String contactName = contactNameFuture.get();

                imageFuture = databaseManager.getProfileImageAsync(contacts.get(i));
                Bitmap contactImage = imageFuture.get();
                contactList.add(new Contact(i, contactName, contactImage));

            }
            //Añadimos los grupos
            contactsFuture = databaseManager.checkUserGroupsAsync(idUser);
            contacts.clear();
            contacts.addAll(contactsFuture.get());

            //Creamos los grupos
            if (numberOfGroups == null) numberOfGroups = contacts;
            if (numberOfGroups.size() != contacts.size()){
                //Igualar grupos
                numberOfGroups = contacts;
                //Cerrar hilo
                user.close = true;
                user.disconnectFromServer();
                threadReceiveMessages.interrupt();
                //Abrir uno nuevo con los grupos nuevos
                if (Chat.signOff != true){
                    userThread(context);
                }
            }
            for (int i = 0; i < contacts.size(); i++) {
                contactNameFuture = databaseManager.getNameGroupAsync(contacts.get(i));
                String groupName = contactNameFuture.get();

                contactList.add(new Contact(i, groupName));

            }

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        return contactList;
    }

    //CARGAR EL CHAT DEL USUARIO ELEGIDO
    public static void loadChat(ChatActivity chatActivity) {
        ChatAdapter chatAdapter = chatActivity.getChatAdapter();
        RecyclerView recyclerView = chatActivity.getRecyclerView();
        //guardar los mensajes
        ArrayList<Message> messages;
        Future<ArrayList<Message>> messagesFuture;

        try {
            messagesFuture = databaseManager.getChatMessagesAsync(idUser, Chat.idContact);
            messages = messagesFuture.get();

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //Creamos el recyclerView
        chatAdapter.setMessageList(messages);
        chatAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(chatAdapter);
        recyclerView.scrollToPosition(messages.size() - 1);

        //Se lo asignamos a nuestra actividad
        chatActivity.setChatAdapter(chatAdapter);
        chatActivity.setRecyclerView(recyclerView);
    }

    //CARGAR EL CHAT DEL GRUPO ELEGIDO
    public static void loadChatGroup(ChatActivity chatActivity) {
        ChatAdapter chatAdapter = chatActivity.getChatAdapter();
        RecyclerView recyclerView = chatActivity.getRecyclerView();
        //guardar los mensajes
        ArrayList<Message> messages;
        Future<ArrayList<Message>> messagesFuture;

        try {
            messagesFuture = databaseManager.getChatMessagesGroupAsync(idUser,Chat.idGroup);
            messages = messagesFuture.get();

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //Creamos el recyclerView
        chatAdapter.setMessageList(messages);
        chatAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(chatAdapter);
        recyclerView.scrollToPosition(messages.size() - 1);

        //Se lo asignamos a nuestra actividad
        chatActivity.setChatAdapter(chatAdapter);
        chatActivity.setRecyclerView(recyclerView);
    }

    //MÉTODO PARA GUARDAR MENSAJES A USUARIOS
    public static void saveMessage(String text, ChatActivity chatActivity) {
        databaseManager.sendMessageAsync(idUser, Chat.idContact, text);
        userSendMessage(text);
        //se cargan los mensajes
        loadChat(chatActivity);

    }

    //MÉTODO PARA ESCRIBIR MENSAJES A USUARIOS
    public static void userSendMessage(String text) {
        try {
            if (!text.equals("")) {
                Future<ArrayList<Integer>> groupsFuture = databaseManager.checkUserGroupsAsync(Chat.idContact);
                user.sendMessage(Chat.idContact, groupsFuture.get(), text);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //MÉTODO PARA GUARDAR MENSAJES A GRUPOS
    public static void saveMessageGroup(String text, ChatActivity chatActivity) {
        text = username + ": " + text;
        databaseManager.sendMessageGroupAsync(idUser, Chat.idGroup, text);
        userSendMessageGroup(text);
        //se cargan los mensajes
        loadChatGroup(chatActivity);

    }

    //MÉTODO PARA ESCRIBIR MENSAJES A GRUPOS
    public static void userSendMessageGroup(String text) {
        if (!text.equals("")) {
            user.sendMessageGroup(Chat.idGroup, text);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //AÑADIR USUARIO
    public static boolean addUser(Context context, DatabaseManager databaseManager, String newUser) {
        int idContactUser = 0;
        Future<Integer> idContactUserFuture;
        //Comprobar que existe
        try {
            idContactUserFuture = databaseManager.checkUserExistsAsync(newUser);
            idContactUser = idContactUserFuture.get();
            //Si no existe, se avisa al usuario
            if (idContactUser == -1) {
                Toast.makeText(context, "El usuario escrito no existe", Toast.LENGTH_SHORT).show();
                return false;
            }
            //Si existe se comprueba si esta agregado
            boolean isAdded;
            Future<Boolean> isAddedFuture;
            //Comprobamos en ambas direcciones si tiene un contacto que agregó o un contacto del que fue agregado
            isAddedFuture = databaseManager.checkContactAddedAsync(idUser, idContactUser);
            isAdded = isAddedFuture.get();

            if (!isAdded) {
                isAddedFuture = databaseManager.checkContactAddedInverseAsync(idUser, idContactUser);
                isAdded = isAddedFuture.get();
            }

            //Si no esta agregado se añade al usuario
            if (!isAdded) {
                databaseManager.addContactToUserAsync(idUser, idContactUser).get();
                //Mostrar mensaje por pantalla
                Toast.makeText(context, "Usuario agregado correctamente", Toast.LENGTH_SHORT).show();
                return true;

            }
            //si no se muestra un mensaje de error de que ya esta agregado
            else
                Toast.makeText(context, "Este usuario ya está en tus contactos", Toast.LENGTH_SHORT).show();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        return false;
    }

    //AÑADIR USUARIO
    public static boolean addGroup(Context context, DatabaseManager databaseManager, String newGroup, ArrayList<Integer> contactsToBeAdded) {
        int idGroup = 0;
        Future<Integer> idGroupFuture;
        boolean existsGroupName;
        boolean existsUserName;
        //Comprobar que existe
        try {

            //Comprobar que el grupo no se llame como un usuario
            Future<Boolean> existsGroupNameFuture = databaseManager.checkNameGroupAsync(newGroup);
            Future<Boolean> existsUserNameFuture = databaseManager.checkUsernameAsync(newGroup);
            existsGroupName = existsGroupNameFuture.get();
            existsUserName = existsUserNameFuture.get();

            if (!existsUserName || existsGroupName) {
                Toast.makeText(context, "Este nombre no está disponible, selecciona otro", Toast.LENGTH_SHORT).show();
                return false;
            }

            Future<Boolean> isCreatedFuture;
            boolean isCreated;

            idGroupFuture = databaseManager.checkGroupsAsync();
            idGroup = idGroupFuture.get() + 1;
            isCreatedFuture = databaseManager.createGroupAsync(newGroup, idGroup, idUser);
            isCreated = isCreatedFuture.get();

            if (isCreated) {
                for (int contact : contactsToBeAdded) {
                    databaseManager.createGroupAsync(newGroup, idGroup, contact);
                }
                Toast.makeText(context, "Grupo creado correctamente", Toast.LENGTH_SHORT).show();
                user.disconnectFromServer();
                userThread(context);
                return true;
            }


        } catch (ExecutionException e) {
            // si sucedió un error en la consulta se avisa al usuario
            Toast.makeText(context, "Error de conexión al intentar agregar un contacto", Toast.LENGTH_SHORT).show();
            return false;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        return false;
    }

    public static boolean deleteContact(Context context, DatabaseManager databaseManager, String contactToBeDeleted) {
        int idContactUser = 0;
        Future<Integer> idContactUserFuture;
        //Comprobar que existe
        try {
            idContactUserFuture = databaseManager.checkUserExistsAsync(contactToBeDeleted);
            idContactUser = idContactUserFuture.get();
            //Si no existe, se avisa al usuario, y si sucedió un error en la consulta también
            if (idContactUser == -1) {
                Toast.makeText(context, "El usuario escrito no está en tus contactos", Toast.LENGTH_SHORT).show();
                return false;
            }
            //Si existe se comprueba si esta agregado
            boolean isAdded;
            Future<Boolean> isAddedFuture;
            isAddedFuture = databaseManager.checkContactAddedAsync(idUser, idContactUser);
            isAdded = isAddedFuture.get();

            if (!isAdded) {
                isAddedFuture = databaseManager.checkContactAddedInverseAsync(idUser, idContactUser);
                isAdded = isAddedFuture.get();
            }
            //Si no esta agregado se añade al usuario
            if (isAdded) {
                databaseManager.deleteContactAsync(idUser, idContactUser).get();
                //Mostrar mensaje por pantalla
                Toast.makeText(context, "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show();
                return true;

            } else
                Toast.makeText(context, "El usuario escrito no está en tus contactos", Toast.LENGTH_SHORT).show();

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public static boolean deleteGroup(Context context, DatabaseManager databaseManager, String groupToBeDeleted) {
        ArrayList<Integer> idGroup;
        Future<ArrayList<Integer>> idGroupFuture;
        Future<String> nameGroupFuture;
        Future<Integer> numberOfUsers;
        String nameGroup;
        //Comprobar que existe
        try {
            idGroupFuture = databaseManager.checkUserGroupsAsync(idUser);
            idGroup = idGroupFuture.get();

            for (int group : idGroup) {
                nameGroupFuture = databaseManager.getNameGroupAsync(group);
                nameGroup = nameGroupFuture.get();
                if (nameGroup.equals(groupToBeDeleted)) {
                    databaseManager.deleteFromGroupAsync(idUser, group);
                    //Comprobar si ese grupo está vacío para eliminarlo
                    numberOfUsers = databaseManager.checkUsersOnGroupsAsync(nameGroup);
                    if (numberOfUsers.get() == 0){
                        databaseManager.deleteChatsofGroupAsync(group);
                    }
                    Toast.makeText(context, "Grupo eliminado correctamente", Toast.LENGTH_SHORT).show();
                    return true;
                }

            }

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static boolean deleteAcount(DatabaseManager databaseManager, int idUser) throws ExecutionException, InterruptedException {
        boolean delete;
        Future<Boolean> deleteFuture;

        deleteFuture = databaseManager.deleteUserAsync(idUser);
        delete = deleteFuture.get();
        if (delete) {
            databaseManager.deleteUserContactAsync(idUser).get();
            return true;
        }
        return false;
    }

    protected static void showSearchDialog(Context context, ContactAdapter contactAdapter, ArrayList<Contact> contactList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //Cambiar el color del titulo
        builder.setTitle(Html.fromHtml("<font color='#2E8B57'>" + "Buscar Contacto" + "</font>"));

        // Crear un EditText para ingresar el nombre del contacto
        final EditText input = new EditText(context);
        input.setHint("Pulsa en buscar para recuperar todos los chats");
        input.setTextSize(14);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Establecer el botón "Buscar" en el diálogo
        builder.setPositiveButton("Buscar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String searchText = input.getText().toString().trim();

                // Realizar la búsqueda en la lista actual de contactos
                ArrayList<Contact> searchResults = new ArrayList<>();
                if (!searchText.equals("")) {
                    for (int i = 0; i < contactList.size(); i++) {
                        if (contactList.get(i).getName().contains(searchText))
                            searchResults.add(contactList.get(i));
                    }
                } else searchResults = contactList;
                // Actualizar el RecyclerView con los resultados de la búsqueda
                contactAdapter.updateContacts(searchResults);
            }
        });

        // Establecer el botón "Cancelar" en el diálogo
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Mostrar el diálogo
        builder.show();
    }

    protected static void windowAddUser(Context context, DatabaseManager databaseManager, ArrayList<Contact> contactList, ContactAdapter contactAdapter, RecyclerView recyclerView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //Cambiar el color del titulo
        builder.setTitle(Html.fromHtml("<font color='#2E8B57'>" + "Agregar Contacto" + "</font>"));

        // Crear un EditText para ingresar el nombre del contacto
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Establecer el botón "Buscar" en el diálogo
        builder.setPositiveButton("Agregar contacto", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String searchText = input.getText().toString().trim();

                // Comprobar si el EditText está vacío
                if (searchText.isEmpty()) {
                    // Mostrar un mensaje de advertencia y permitir al usuario introducir otro dato
                    Toast.makeText(context, "No ha escrito nada en el campo", Toast.LENGTH_SHORT).show();
                } else {
                    boolean added = addUser(context, databaseManager, searchText);
                    if (added) {
                        //Actualizar la vista
                        if (!contactList.isEmpty()) {
                            contactList.clear();
                        }
                        try {
                            Future<Bitmap> imageFuture;
                            Future<String> nameFuture;
                            Bitmap image;
                            String name;

                            ArrayList<Integer> contacts;
                            Future<ArrayList<Integer>> contactsFuture;
                            //Añade a los chats los usuarios que haya agregado y que le hayan agregado al usuario
                            contactsFuture = databaseManager.checkContactsAsync(idUser);
                            contacts = contactsFuture.get();
                            contactsFuture = databaseManager.checkContactsInverseAsync(idUser);
                            contacts.addAll(contactsFuture.get());

                            for (int i = 0; i < contacts.size(); i++) {
                                //Conseguir la imagen de perfil del usuario
                                imageFuture = databaseManager.getProfileImageAsync(contacts.get(i));
                                image = imageFuture.get();

                                nameFuture = databaseManager.returnUsernameByIdAsync(contacts.get(i));
                                name = nameFuture.get();

                                contactList.add(new Contact(i, name, image));
                            }
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Future<ArrayList<Integer>> groupsFuture;
                            ArrayList<Integer> groups;

                            //Añade a los chats los grupos que haya creado o donde este el usuario
                            groupsFuture = databaseManager.checkUserGroupsAsync(idUser);
                            groups = groupsFuture.get();

                            for (int i = 0; i < groups.size(); i++) {
                                String name = null;
                                Future<String> nameFuture;

                                nameFuture = databaseManager.getNameGroupAsync(groups.get(i));
                                name = nameFuture.get();
                                contactList.add(new Contact(i, name));
                            }

                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        contactAdapter.updateContacts(contactList);
                        recyclerView.setAdapter(contactAdapter);

                    } else
                        windowAddUser(context, databaseManager, contactList, contactAdapter, recyclerView);
                }
            }
        });

        // Establecer el botón "Cancelar" en el diálogo
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Mostrar el diálogo
        builder.show();
    }

    protected static void windowAddGroup(Context context, DatabaseManager databaseManager, ArrayList<Contact> contactList, ContactAdapter contactAdapter, RecyclerView recyclerView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //Cambiar el color del titulo
        builder.setTitle(Html.fromHtml("<font color='#2E8B57'>" + "Agregar Grupo" + "</font>"));


        // Crear un EditText para ingresar el nombre del contacto y el nombre del grupo
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Establecer el botón "Crear grupo" en el diálogo
        builder.setPositiveButton("Crear Grupo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String searchText = input.getText().toString().trim();

                // Comprobar si el EditText está vacío
                if (searchText.isEmpty()) {
                    // Mostrar un mensaje de advertencia y permitir al usuario introducir otro dato
                    Toast.makeText(context, "No ha escrito nada en el campo", Toast.LENGTH_SHORT).show();
                    windowAddGroup(context, databaseManager, contactList, contactAdapter, recyclerView);
                } else {
                    boolean added = false;
                    if (contactsToBeAdded.size() == 0) {
                        Toast.makeText(context, "Debes agregar al menos un contacto primero", Toast.LENGTH_SHORT).show();
                        windowAddGroup(context, databaseManager, contactList, contactAdapter, recyclerView);
                    } else
                        added = addGroup(context, databaseManager, searchText, contactsToBeAdded);

                    if (added) {
                        //Reiniciar la lista
                        contactsToBeAdded = new ArrayList<>();
                        //Actualizar la vista
                        if (!contactList.isEmpty()) {
                            contactList.clear();
                        }
                        try {
                            Future<Bitmap> imageFuture;
                            Future<String> nameFuture;
                            Bitmap image;
                            String name;

                            ArrayList<Integer> contacts;
                            Future<ArrayList<Integer>> contactsFuture;
                            //Añade a los chats los usuarios que haya agregado y que le hayan agregado al usuario
                            contactsFuture = databaseManager.checkContactsAsync(idUser);
                            contacts = contactsFuture.get();
                            contactsFuture = databaseManager.checkContactsInverseAsync(idUser);
                            contacts.addAll(contactsFuture.get());

                            for (int i = 0; i < contacts.size(); i++) {
                                //Conseguir la imagen de perfil del usuario
                                imageFuture = databaseManager.getProfileImageAsync(contacts.get(i));
                                image = imageFuture.get();

                                nameFuture = databaseManager.returnUsernameByIdAsync(contacts.get(i));
                                name = nameFuture.get();

                                contactList.add(new Contact(i, name, image));
                            }
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Future<ArrayList<Integer>> groupsFuture;
                            ArrayList<Integer> groups;

                            //Añade a los chats los grupos que haya creado o donde este el usuario
                            groupsFuture = databaseManager.checkUserGroupsAsync(idUser);
                            groups = groupsFuture.get();

                            for (int i = 0; i < groups.size(); i++) {
                                String name;
                                Future<String> nameFuture;

                                nameFuture = databaseManager.getNameGroupAsync(groups.get(i));
                                name = nameFuture.get();
                                contactList.add(new Contact(i, name));
                            }

                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }


                        contactAdapter.updateContacts(contactList);
                        recyclerView.setAdapter(contactAdapter);
                    } else
                        windowAddGroup(context, databaseManager, contactList, contactAdapter, recyclerView);
                }
            }
        });
        // Establecer el botón "Añadir usuario" en el diálogo
        builder.setNeutralButton("Añadir usuario", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String addUserText = input.getText().toString().trim();
                Future<ArrayList<Integer>> arrayContactFuture;
                Future<ArrayList<Integer>> arrayContactAddedFuture;
                ArrayList<Integer> arrayContact;
                ArrayList<Integer> arrayContactAdded;
                Future<Integer> idContactFuture;
                int idContact;
                boolean haveContact = false;
                try {
                    //Guardar los contactos para comprobar si el que quiere agregar lo tiene el usuario
                    arrayContactFuture = databaseManager.checkContactsAsync(idUser);
                    arrayContactAddedFuture = databaseManager.checkContactsInverseAsync(idUser);
                    idContactFuture = databaseManager.returnIdAsync(addUserText);

                    arrayContact = arrayContactFuture.get();
                    arrayContactAdded = arrayContactAddedFuture.get();
                    idContact = idContactFuture.get();

                    for (Integer contact: arrayContact){
                        if (contact == idContact) haveContact = true;
                    }
                    if (!haveContact) {
                        for (Integer contact: arrayContactAdded){
                            if (contact == idContact) haveContact = true;
                        }
                    }

                    //Avisar si el usuario esta en los contactos o no
                    if (!haveContact) {
                        Toast.makeText(context, "El contacto no está en tu lista de contactos", Toast.LENGTH_SHORT).show();
                        windowAddGroup(context, databaseManager, contactList, contactAdapter, recyclerView);
                    } else {
                        contactsToBeAdded.add(idContact);
                        Toast.makeText(context, "Contacto preparado para el grupo", Toast.LENGTH_SHORT).show();
                        windowAddGroup(context, databaseManager, contactList, contactAdapter, recyclerView);
                    }

                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        // Establecer el botón "Cancelar" en el diálogo
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Mostrar el diálogo
        builder.show();
    }

    protected static void windowDeleteContact(Context context, DatabaseManager databaseManager, ArrayList<Contact> contactList, ContactAdapter contactAdapter, RecyclerView recyclerView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //Cambiar el color del titulo
        builder.setTitle(Html.fromHtml("<font color='#2E8B57'>" + "Escriba el contacto que desea eliminar" + "</font>"));

        // Crear un EditText para ingresar el nombre del contacto
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Establecer el botón "Buscar" en el diálogo
        builder.setPositiveButton("Borrar contacto", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String searchText = input.getText().toString().trim();

                // Comprobar si el EditText está vacío
                if (searchText.isEmpty()) {
                    // Mostrar un mensaje de advertencia y permitir al usuario introducir otro dato
                    Toast.makeText(context, "No ha escrito nada en el campo", Toast.LENGTH_SHORT).show();
                    windowDeleteContact(context, databaseManager, contactList, contactAdapter, recyclerView);
                } else {
                    if (deleteContact(context, databaseManager, searchText)) {
                        //Actualizar la vista
                        if (!contactList.isEmpty()) {
                            contactList.clear();
                        }
                        try {
                            Future<Bitmap> imageFuture;
                            Future<String> nameFuture;
                            Bitmap image;
                            String name;

                            ArrayList<Integer> contacts;
                            Future<ArrayList<Integer>> contactsFuture;
                            //Añade a los chats los usuarios que haya agregado y que le hayan agregado al usuario
                            contactsFuture = databaseManager.checkContactsAsync(idUser);
                            contacts = contactsFuture.get();
                            contactsFuture = databaseManager.checkContactsInverseAsync(idUser);
                            contacts.addAll(contactsFuture.get());

                            for (int i = 0; i < contacts.size(); i++) {
                                //Conseguir la imagen de perfil del usuario
                                imageFuture = databaseManager.getProfileImageAsync(contacts.get(i));
                                image = imageFuture.get();

                                nameFuture = databaseManager.returnUsernameByIdAsync(contacts.get(i));
                                name = nameFuture.get();

                                contactList.add(new Contact(i, name, image));
                            }
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Future<ArrayList<Integer>> groupsFuture;
                            ArrayList<Integer> groups;

                            //Añade a los chats los grupos que haya creado o donde este el usuario
                            groupsFuture = databaseManager.checkUserGroupsAsync(idUser);
                            groups = groupsFuture.get();

                            for (int i = 0; i < groups.size(); i++) {
                                String name = null;
                                Future<String> nameFuture;

                                nameFuture = databaseManager.getNameGroupAsync(groups.get(i));
                                name = nameFuture.get();
                                contactList.add(new Contact(i, name));
                            }

                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        contactAdapter.updateContacts(contactList);
                        recyclerView.setAdapter(contactAdapter);
                    } else
                        windowDeleteContact(context, databaseManager, contactList, contactAdapter, recyclerView);
                }
            }
        });

        // Establecer el botón "Cancelar" en el diálogo
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Mostrar el diálogo
        builder.show();
    }

    protected static void windowDeleteGroup(Context context, DatabaseManager databaseManager, ArrayList<Contact> contactList, ContactAdapter contactAdapter, RecyclerView recyclerView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //Cambiar el color del titulo
        builder.setTitle(Html.fromHtml("<font color='#2E8B57'>" + "Escriba el grupo que desea eliminar" + "</font>"));

        // Crear un EditText para ingresar el nombre del contacto
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Establecer el botón "Buscar" en el diálogo
        builder.setPositiveButton("Borrar Grupo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String searchText = input.getText().toString().trim();

                // Comprobar si el EditText está vacío
                if (searchText.isEmpty()) {
                    // Mostrar un mensaje de advertencia y permitir al usuario introducir otro dato
                    Toast.makeText(context, "No ha escrito nada en el campo", Toast.LENGTH_SHORT).show();
                } else {
                    if (deleteGroup(context, databaseManager, searchText)) {
                        //Actualizar la vista
                        if (!contactList.isEmpty()) {
                            contactList.clear();
                        }
                        try {
                            Future<Bitmap> imageFuture;
                            Future<String> nameFuture;
                            Bitmap image;
                            String name;

                            ArrayList<Integer> contacts;
                            Future<ArrayList<Integer>> contactsFuture;
                            //Añade a los chats los usuarios que haya agregado y que le hayan agregado al usuario
                            contactsFuture = databaseManager.checkContactsAsync(idUser);
                            contacts = contactsFuture.get();
                            contactsFuture = databaseManager.checkContactsInverseAsync(idUser);
                            contacts.addAll(contactsFuture.get());

                            for (int i = 0; i < contacts.size(); i++) {
                                //Conseguir la imagen de perfil del usuario
                                imageFuture = databaseManager.getProfileImageAsync(contacts.get(i));
                                image = imageFuture.get();

                                nameFuture = databaseManager.returnUsernameByIdAsync(contacts.get(i));
                                name = nameFuture.get();

                                contactList.add(new Contact(i, name, image));
                            }
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Future<ArrayList<Integer>> groupsFuture;
                            ArrayList<Integer> groups;

                            //Añade a los chats los grupos que haya creado o donde este el usuario
                            groupsFuture = databaseManager.checkUserGroupsAsync(idUser);
                            groups = groupsFuture.get();

                            for (int i = 0; i < groups.size(); i++) {
                                String name = null;
                                Future<String> nameFuture;

                                nameFuture = databaseManager.getNameGroupAsync(groups.get(i));
                                name = nameFuture.get();
                                contactList.add(new Contact(i, name));
                            }

                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        contactAdapter.updateContacts(contactList);
                        recyclerView.setAdapter(contactAdapter);
                    } else {
                        Toast.makeText(context, "El Grupo escrito no existe", Toast.LENGTH_SHORT).show();
                        windowDeleteGroup(context, databaseManager, contactList, contactAdapter, recyclerView);
                    }
                }
            }
        });

        // Establecer el botón "Cancelar" en el diálogo
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Mostrar el diálogo
        builder.show();
    }

    protected static void windowDeleteAccount(Context context, DatabaseManager databaseManager, int idUser) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Cambiar el color del titulo
        builder.setTitle(Html.fromHtml("<font color='#2E8B57'>" + "¿Seguro que quieres eliminar la cuenta?" + "</font>"));

        // Establecer el botón "Eliminar Cuenta" en el diálogo
        builder.setPositiveButton("Eliminar Cuenta", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Mostrar un mensaje de advertencia
                try {
                    if (deleteAcount(databaseManager, idUser)) {
                        Intent intent = new Intent(context, Login.class);
                        Toast.makeText(context, "¡Esperamos volver a verte pronto!", Toast.LENGTH_SHORT).show();
                        username = "empty";
                        Mediator.user.disconnectFromServer();
                        databaseManager.shutdown();
                        context.startActivity(intent);
                    } else
                        Toast.makeText(context, "Error al borrar la cuenta", Toast.LENGTH_SHORT).show();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        });

        // Establecer el botón "Cancelar" en el diálogo
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Mostrar el diálogo
        builder.show();
    }

    //Ventana de cargando
    public static ProgressDialog loadingScreen(Context context) {
        // Crear y configurar el ProgressDialog
        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Por favor, espere...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false); // Evita que el usuario lo cancele
        return progressDialog;
    }

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    public static Bitmap resizeImage(Bitmap bitmap, int targetWidth, int targetHeight) {
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

}
