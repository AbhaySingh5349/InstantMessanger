package com.example.messangerapp.model;

public class FindFriendsModelClass {

    String Name, PhotoURL, UserId;

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

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public FindFriendsModelClass(String name, String photoURL, String userId) {
        Name = name;
        PhotoURL = photoURL;
        UserId = userId;
    }

    public FindFriendsModelClass() {
    }
}
