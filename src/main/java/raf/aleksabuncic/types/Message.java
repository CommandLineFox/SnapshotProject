package raf.aleksabuncic.types;

import java.io.Serializable;

public record Message(String type, int senderId, String content) implements Serializable {

    @Override
    public String toString() {
        return "[" + type + "] from Node " + senderId + ": " + content;
    }
}
