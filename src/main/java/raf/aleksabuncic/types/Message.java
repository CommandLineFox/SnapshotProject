package raf.aleksabuncic.types;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class Message implements Serializable {
    private final String type;
    private final int senderId;
    private final String content;

    public Message(String type, int senderId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.content = content;
    }

    @Override
    public String toString() {
        return "[" + type + "] from Node " + senderId + ": " + content;
    }
}
