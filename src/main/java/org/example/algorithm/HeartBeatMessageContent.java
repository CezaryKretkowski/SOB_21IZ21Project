package org.example.algorithm;
import com.google.gson.Gson;

public class HeartBeatMessageContent {
    public String msgContent;
    public int hostNumber;

    public HeartBeatMessageContent(int hostNumber, String content) {
        this.hostNumber = hostNumber;
        this.msgContent = content;
    }
    public HeartBeatMessageContent(String jsonString) {
        HeartBeatMessageContent massage = new Gson().fromJson(jsonString, HeartBeatMessageContent.class);
        this.msgContent = massage.msgContent;
        this.hostNumber = massage.hostNumber;
    }
    public String toJson() {
        return new Gson().toJson(this);
    }
}
