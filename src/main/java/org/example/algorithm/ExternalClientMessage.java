package org.example.algorithm;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class ExternalClientMessage {
    @SerializedName("Content")
    public String content;
    public boolean isForwarder;
    public boolean isBroadcast;
    public String address;
    public int port;

    public ExternalClientMessage(int port, String address, boolean isForwarder, boolean isBroadcast, String content) {
        this.port = port;
        this.address = address;
        this.isForwarder = isForwarder;
        this.isBroadcast = isBroadcast;
        this.content = content;
    }

    public ExternalClientMessage(String jsonString) {
        ExternalClientMessage message =  new Gson().fromJson(jsonString, ExternalClientMessage.class);
        content = message.content;
        isForwarder = message.isForwarder;
        isBroadcast = message.isBroadcast;
        address = message.address;
        port = message.port;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
