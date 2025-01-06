package org.example.algorithm;

import com.google.gson.Gson;

public class ExternalClientMessage {
    public String content;
    public boolean isForwarder;
    public String address;
    public int port;

    public ExternalClientMessage(int port, String address, boolean isForwarder, String content) {
        this.port = port;
        this.address = address;
        this.isForwarder = isForwarder;
        this.content = content;
    }

    public ExternalClientMessage(String jsonString) {
        ExternalClientMessage message =  new Gson().fromJson(jsonString, ExternalClientMessage.class);
        content = message.content;
        isForwarder = message.isForwarder;
        address = message.address;
        port = message.port;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
