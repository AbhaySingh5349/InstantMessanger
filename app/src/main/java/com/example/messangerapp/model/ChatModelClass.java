package com.example.messangerapp.model;

public class ChatModelClass {

    String UserId, Name, PhotoURL, UnreadCount, LastMessage, LastMessageTime;

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getPhotoURL() {
        return PhotoURL;
    }

    public void setPhotoURL(String photoURL) {
        PhotoURL = photoURL;
    }

    public String getUnreadCount() {
        return UnreadCount;
    }

    public void setUnreadCount(String unreadCount) {
        UnreadCount = unreadCount;
    }

    public String getLastMessage() {
        return LastMessage;
    }

    public void setLastMessage(String lastMessage) {
        LastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return LastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        LastMessageTime = lastMessageTime;
    }

    public ChatModelClass(String userId, String name, String photoURL, String unreadCount, String lastMessage, String lastMessageTime) {
        UserId = userId;
        Name = name;
        PhotoURL = photoURL;
        UnreadCount = unreadCount;
        LastMessage = lastMessage;
        LastMessageTime = lastMessageTime;
    }

    public ChatModelClass() {
    }
}
