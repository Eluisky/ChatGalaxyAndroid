package Objects;

public class Message {
    int userId;
    int contactId;
    String textMessage;

    public Message(int userId, int contactId, String textMessage) {
        this.userId = userId;
        this.contactId = contactId;
        this.textMessage = textMessage;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getContactId() {
        return contactId;
    }

    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public void setTextMessage(String textMessage) {
        this.textMessage = textMessage;
    }
}
