package com.cognition.android.mailboxapp;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Profile {

    @SerializedName("subject")
    @Expose
    private String subject;

    @SerializedName("sender")
    @Expose
    private String sender;

    @SerializedName("message")
    @Expose
    private String message;

    @SerializedName("id")
    @Expose
    private String Id;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }
}