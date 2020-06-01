package com.main;

public interface ChatServerApi {

    public ChatDataSingleton refresh();
    public boolean subscribe(String username);
    public void sendMessage(Message message);
    public void userLeft(String username);
}
