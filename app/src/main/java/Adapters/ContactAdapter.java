package Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ielena.myapplication.Chat;
import com.ielena.myapplication.ChatActivity;
import com.ielena.myapplication.R;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import Objects.Contact;
import Server.DatabaseManager;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<Contact> contactList;
    private Context context;
    private DatabaseManager databaseManager;
    private int userGroupLastMessage;
    private String userOrGroup;


    // Constructor
    public ContactAdapter(List<Contact> contactList, Context context) {
        this.contactList = contactList;
        this.context = context;
        databaseManager = new DatabaseManager();
    }

    // Método llamado cuando se necesita crear una nueva vista para un elemento de la lista
    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    // Método llamado para asociar datos a una vista en una posición específica
    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        try {
            Future<Integer> IdFuture;
                if (contact.getProfile_image() == null){
                    IdFuture = databaseManager.getIdGroupAsync(contact.getName());
                    userGroupLastMessage = IdFuture.get();
                    userOrGroup = "group";
                }
                else {
                    IdFuture = databaseManager.returnIdAsync(contact.getName());
                    userGroupLastMessage = IdFuture.get();
                    userOrGroup = "user";
                }

        }catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        holder.bind(contact);

        // Establecer el OnClickListener en cada ViewHolder
        holder.itemView.setOnClickListener( v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            int id;
            Future<Integer> IdFuture;
            try {
                if (contact.getProfile_image() == null){
                    IdFuture = databaseManager.getIdGroupAsync(contact.getName());
                    id = IdFuture.get();
                    intent.putExtra("isGroup", "isGroup");
                    //Guardar el grupo que se está usando en la clase Chat
                    Chat.idGroup = id;
                }
                else {
                    IdFuture = databaseManager.returnIdAsync(contact.getName());
                    id = IdFuture.get();
                    intent.putExtra("id_user", Chat.idUser);
                    intent.putExtra("isContact", "isContact");
                    //Guardar el contacto que se esta usando en la clase Chat
                    Chat.idContact = id;
                }
                context.startActivity(intent);

            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Método que devuelve el número total de elementos en la lista
    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public void updateContacts(List<Contact> newContactList) {
        contactList = newContactList;
        notifyDataSetChanged();
    }

    // Clase ViewHolder que representa una única vista de un elemento de la lista
    public class ContactViewHolder extends RecyclerView.ViewHolder {

        private TextView nameTextView;
        private TextView lastMessage;

        private ImageView imageView;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.text_contact_name);
            lastMessage = itemView.findViewById(R.id.text_last_message);
            imageView = itemView.findViewById(R.id.image_contact);
        }

        // Método para asociar datos del contacto a los elementos de la vista
        public void bind(Contact contact) {
            nameTextView.setText(contact.getName());
            imageView.setImageBitmap(contact.getProfile_image());
            try {
                if (userOrGroup.equals("user")){
                    Future<String> lastMessageFuture = databaseManager.getLastMessageAsync(Chat.idUser,userGroupLastMessage);
                    lastMessage.setText(lastMessageFuture.get());
                }
                else if (userOrGroup.equals("group")){
                    Future<String> lastMessageFuture = databaseManager.getLastMessageGroupAsync(userGroupLastMessage);
                    lastMessage.setText(lastMessageFuture.get());
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}