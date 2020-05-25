package com.main;

import java.io.Serializable;
import java.time.LocalTime;

public class Message implements Serializable, Comparable<Message> {

    private String username;
    private String text;
    private String timestamp;

    public Message(String username, String text) {
        this.username = username;
        this.text = text;
    }

    public String getMessageFullContent() {
        return "[" + timestamp + "] " + username + ": " + text;
    }

    public String getMessageText() {
        return text;
    }

    public void setMessageTimestamp(LocalTime now) {
        this.timestamp = "" + now.getHour() + ":" + now.getMinute() + ":" + now.getSecond();
    }

    @Override
    public int compareTo(Message o) {
        return timestamp.compareTo(o.timestamp);
    }
}
