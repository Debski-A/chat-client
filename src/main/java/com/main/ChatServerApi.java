package com.main;

public interface ChatServerApi {

    public ChatData refresh();
    public boolean subscribe(String username);
    public void sendMessage(Message message);
    public void userLeft(String username);
}
