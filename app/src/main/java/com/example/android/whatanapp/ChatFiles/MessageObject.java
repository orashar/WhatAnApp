package com.example.android.whatanapp.ChatFiles;

public class MessageObject {
    private String messageId, message, senderId;

    public MessageObject(String messageId, String message, String senderId) {
        this.messageId = messageId;
        this.message = message;
        this.senderId = senderId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public String getSenderId() {
        return senderId;
    }
}
