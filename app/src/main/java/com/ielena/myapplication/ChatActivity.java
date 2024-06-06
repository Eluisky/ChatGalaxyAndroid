package com.ielena.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import Adapters.ChatAdapter;

public class ChatActivity extends AppCompatActivity {

    protected  RecyclerView recyclerView;
    public ChatAdapter chatAdapter;
    private EditText editTextMessage;
    private Button buttonSend;
    public static ChatActivity chatActivity;
    public static String messageTo = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getSupportActionBar().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background));

        recyclerView = findViewById(R.id.recycler_view_chat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Inicializa el adaptador con una lista vacía
        chatAdapter = new ChatAdapter(new ArrayList<>(), Chat.idUser);
        recyclerView.setAdapter(chatAdapter);

        //crear el objeto de la clase para poder llamar al metodo cargar mensajes en el hilo
        chatActivity = this;
        if (getIntent().getStringExtra("isContact") != null){
            messageTo = getIntent().getStringExtra("isContact");
            loadMessages();
        }
        else if (getIntent().getStringExtra("isGroup") != null){
            messageTo = getIntent().getStringExtra("isGroup");
            loadMessagesGroup();
        }

        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);

        buttonSend.setOnClickListener(v -> sendMessage());

    }

    public void loadMessages() {
        Mediator.loadChat(chatActivity);

    }
    public void loadMessagesGroup() {
        Mediator.loadChatGroup(chatActivity);

    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString();
        if (!messageText.isEmpty()) {
            if (messageTo.equals("isContact")){
                Mediator.saveMessage(messageText,this);
                editTextMessage.setText("");
            }
            else if (messageTo.equals("isGroup")){
                Mediator.saveMessageGroup(messageText,this);
                editTextMessage.setText("");
            }
        }
    }
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public ChatAdapter getChatAdapter() {
        return chatAdapter;
    }

    public void setChatAdapter(ChatAdapter chatAdapter) {
        this.chatAdapter = chatAdapter;
    }

}