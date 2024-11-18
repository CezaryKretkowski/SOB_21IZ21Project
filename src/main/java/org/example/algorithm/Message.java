package org.example.algorithm;

import com.google.gson.Gson;

public class Message {
    public String token;
    public Type type;
    public String content;

    public Message(){}

    public Message(String token,Type type,String content){
        this.content = content;
        this.type = type;
        this.token = token;
    }
    public Message (String jsonString){
        Message massage = new Gson().fromJson(jsonString, Message.class);
        this.content = massage.content;
        this.token = massage.token;
        this.type = massage.type;
    }
    public String toJson(){
        return new Gson().toJson(this);
    }
}
