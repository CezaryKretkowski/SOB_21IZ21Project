package org.example.algorithm;

import com.google.gson.Gson;

public class Message {
    public String token;
    public Type type;
    public String content;
    private static final String SECRET_KEY = "shared-secret-key";

    public Message(){}

    public Message(Type type,String content){
        this.content = content;
        this.type = type;
        this.token = generateToken();
    }
    public Message (String jsonString){
        Message massage = new Gson().fromJson(jsonString, Message.class);
        this.content = massage.content;
        this.token = massage.token;
        this.type = massage.type;
    }

    private String generateToken() {
        return SimpleHash.hash(content, SECRET_KEY);
    }

    public boolean verifyToken() {
        return SimpleHash.verifyHash(content, SECRET_KEY, token);
    }

    public String toJson() {
        if (token == null || token.isEmpty()) {
            token = generateToken();
        }
        return new Gson().toJson(this);
    }
}
