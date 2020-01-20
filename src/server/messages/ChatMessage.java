package server.messages;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    private String message;
    private String who;

    public ChatMessage(String message , String who) {
        this.message = message;
        this.who = who;
    }

    public String getMessage() {
        return message;
    }
    public String getWho() {
        return who;
    }
}
