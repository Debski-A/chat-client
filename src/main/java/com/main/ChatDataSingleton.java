package com.main;

import java.io.Serializable;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public class ChatDataSingleton implements Serializable {

    // Klasa jest singletonem, bo bedzie wspodzielona miedzy wywolaniami Hessian i XML RPC
    // singleton oznacza, ze podczas "życia" aplikacja będzie dostępna tylko JEDNA instancja tej klasy
    private static ChatDataSingleton single_instance = null;
    private Set<String> users = new HashSet<>();
    private NavigableSet<Message> messages = new TreeSet<>();

    private ChatDataSingleton() {
    }

    public static ChatDataSingleton ChatData() {
        if (single_instance == null) {
            single_instance = new ChatDataSingleton();
        }
        return single_instance;
    }

    public Set<String> getUsers() {
        return users;
    }

    public NavigableSet<Message> getMessages() {
        return messages;
    }
}
