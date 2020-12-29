package com.example.messangerapp.model;

public class MessageModelClass {

    private String messageId, messageSenderId, messageType, messageValue;
    private long messageTime;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageSenderId() {
        return messageSenderId;
    }

    public void setMessageSenderId(String messageSenderId) {
        this.messageSenderId = messageSenderId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageValue() {
        return messageValue;
    }

    public void setMessageValue(String messageValue) {
        this.messageValue = messageValue;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public MessageModelClass(String messageId, String messageSenderId, String messageType, String messageValue, long messageTime) {
        this.messageId = messageId;
        this.messageSenderId = messageSenderId;
        this.messageType = messageType;
        this.messageValue = messageValue;
        this.messageTime = messageTime;
    }

    public MessageModelClass() {
    }
}
