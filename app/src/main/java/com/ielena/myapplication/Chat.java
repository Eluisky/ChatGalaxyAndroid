package com.ielena.myapplication;

import static com.ielena.myapplication.Mediator.PICK_IMAGE_REQUEST;
import static com.ielena.myapplication.Mediator.user;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import Adapters.ContactAdapter;
import Objects.Contact;
import Server.DatabaseManager;

public class Chat extends AppCompatActivity {
    private DatabaseManager databaseManager;
    private RecyclerView recyclerView;
    private ContactAdapter contactAdapter;
    private ArrayList<Contact> contactList = new ArrayList<>();
    public static int idUser = 0;
    public static int idContact = 0;
    public static int idGroup = 0;
    public static String username = "empty";
    public static ImageView profileImage;
    public static TextView welcome;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_view);
        getSupportActionBar().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background));

        //Si el nombre de usuario vale "empty", se recupera el nombre del usuario del intent
        //En caso contrario quiere decir que el usuario ha vuelto atrás de la vista de chats
        //por lo que al recuperar el nombre devolverá nulo ya que intentará sacar el nombre de un intent nulo
        if (username.equals("empty"))username = getIntent().getStringExtra("username");

        databaseManager = new DatabaseManager();
        //Pasamos el context al mediador para que segun en que ventana se esté, tenga su ventana de cargando
        Mediator.context = this;

        recyclerView = findViewById(R.id.recycler_view_conversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //Escribir el nombre en pantalla
        welcome = findViewById(R.id.welcome);
        //Mostrar la imagen de perfil en pantalla
        profileImage = findViewById(R.id.profileImage);

        // Asignamos el nombre a la vista de chats
        welcome.setText("¡Bienvenido/a " + username + "!");
        contactList = Mediator.createChat(username);
        contactAdapter = new ContactAdapter(contactList, this);
        recyclerView.setAdapter(contactAdapter);

        user.chatContext = this;

    }
    public void updateRecyclerView() {
        runOnUiThread(() -> {
            contactList.clear();
            contactList.addAll(Mediator.createChat(username));
            recyclerView.setAdapter(contactAdapter);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            Mediator.showSearchDialog(this, contactAdapter, contactList);
            return true;
        }
        if (id == R.id.action_signOff) {
            //Detener hilo al salir de la actividad
            onStop();
            username = "empty";
            user.disconnectFromServer();
            databaseManager.shutdown();
            Intent intent = new Intent(Chat.this, Login.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_changeImage) {
            openImagePicker();
            return true;
        }
        if (id == R.id.action_createGroup) {
            Mediator.windowAddGroup(this, databaseManager, contactList, contactAdapter, recyclerView);
            return true;
        }
        if (id == R.id.action_deleteGroup) {
            Mediator.windowDeleteGroup(this, databaseManager, contactList, contactAdapter, recyclerView);
            return true;
        }
        if (id == R.id.action_addContact) {
            Mediator.windowAddUser(this, databaseManager, contactList, contactAdapter, recyclerView);
            return true;
        }
        if (id == R.id.action_deleteContact) {
            Mediator.windowDeleteContact(this, databaseManager, contactList, contactAdapter, recyclerView);
            return true;
        }
        if (id == R.id.action_deleteAccount) {
            Mediator.windowDeleteAccount(this, databaseManager, idUser);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                Bitmap resizedBitmap = Mediator.resizeImage(bitmap, 100, 100);
                byte[] arrayImage = Mediator.bitmapToBytes(resizedBitmap);

                Future<Boolean> changedFuture;
                changedFuture = databaseManager.changeImageAsync(arrayImage, idUser);
                boolean changed = changedFuture.get();
                if (changed) {
                    profileImage.setImageBitmap(resizedBitmap);
                    Toast.makeText(this, "Imagen de perfil actualizada correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al actualizar la imagen de perfil", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}