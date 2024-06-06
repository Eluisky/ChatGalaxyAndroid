package Adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ielena.myapplication.R;

import java.util.ArrayList;

import Objects.Message;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private ArrayList<Message> messageList;
    private int currentUserId;

    public ChatAdapter(ArrayList<Message> messageList, int currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    public void setMessageList(ArrayList<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {

        private TextView textMessage;

        public ChatViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
        }

        public void bind(Message message) {
            textMessage.setText(message.getTextMessage());
            // Alinear el texto basado en el usuario que envió el mensaje
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textMessage.getLayoutParams();
            if (message.getUserId() == currentUserId) {
                params.gravity = Gravity.END;
                textMessage.setLayoutParams(params);
                textMessage.setBackgroundResource(R.drawable.bg_message_user);
            } else {
                params.gravity = Gravity.START;
                textMessage.setLayoutParams(params);
                textMessage.setBackgroundResource(R.drawable.bg_message_contact);
            }
        }
    }
}
