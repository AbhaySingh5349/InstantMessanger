package com.example.messangerapp.model;

public class SelectFriendModelClass {

    private String UserId, Name, PhotoURL;

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

    public SelectFriendModelClass(String userId, String name, String photoURL) {
        UserId = userId;
        Name = name;
        PhotoURL = photoURL;
    }

    public SelectFriendModelClass() {
    }
}
