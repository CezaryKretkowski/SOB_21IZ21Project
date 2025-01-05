package org.example.algorithm;

import com.google.gson.Gson;

public class Message {
    public String token;
    public Type type;
    public String content;
    public static String SECRET_KEY = "shared-secret-key";

    public Message() {}

    public Message(Type type, String content) {
        this.content = content;
        this.type = type;
        this.token = generateToken();
    }

    public Message(String jsonString) {
        Message message = new Gson().fromJson(jsonString, Message.class);
        this.content = message.content;
        this.token = message.token;
        this.type = message.type;
    }

    private String generateToken() {
        return CustomHash.hash(content, SECRET_KEY);
    }

    public boolean verifyToken() {
        return CustomHash.verifyHash(content, SECRET_KEY, token);
    }

    public String toJson() {
        if (token == null || token.isEmpty()) {
            token = generateToken();
        }
        return new Gson().toJson(this);
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
