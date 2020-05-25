package com.main;

import java.io.Serializable;
import java.util.*;

public class ChatData implements Serializable {

    private Set<String> users = new HashSet<>();
    private NavigableSet<Message> messages = new TreeSet<>();

    public Set<String> getUsers() {
        return users;
    }

    public NavigableSet<Message> getMessages() {
        return messages;
    }
}
