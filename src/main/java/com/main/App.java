package com.main;

import com.caucho.hessian.client.HessianProxyFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;


/**
 * JavaFX App
 */
public class App extends Application {

    private Stage stage;
    private ChatServerApi chatServer;
    private String username;
    private volatile boolean isWindowClosed = false;
    private ChatData chatData;
    private ListView<String> users;
    private ObservableList<String> innerUsersList;
    private ListView<String> messages;
    private ObservableList<String> innerMessagesList;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws MalformedURLException {
        this.stage = stage;
        String serverEndpoint = "http://localhost:8080/chat-server/chat";
        HessianProxyFactory hessianProxyFactory = new HessianProxyFactory();
        chatServer = (ChatServerApi) hessianProxyFactory.create(ChatServerApi.class, serverEndpoint);
        stage.setOnCloseRequest(e -> {
            isWindowClosed = true;
            chatServer.userLeft(username);
        });
        Parent chatEntrance = createChatEntrance();
        stage.setScene(new Scene(chatEntrance));
        stage.show();
    }

    private Parent createChatEntrance() {
        VBox vBoxRoot = new VBox();
        vBoxRoot.setPrefSize(450, 200);

        Text entranceMessage = new Text("Wpisz swój nick");
        entranceMessage.setFont(Font.font(36));
        entranceMessage.setTextAlignment(TextAlignment.CENTER);
        HBox hBoxText = new HBox(entranceMessage);
        hBoxText.setPrefSize(450, 100);
        hBoxText.setAlignment(Pos.CENTER);

        TextField input = new TextField("");
        input.setPrefWidth(300);
        Button okButton = new Button("Ok");
        okButton.setOnMouseClicked(e -> {
            if (input.getText().isEmpty()) {
                entranceMessage.setText("Nick nie może być pusty");
            } else if (!chatServer.subscribe(input.getText())) {
                entranceMessage.setText("Nick już jest zajęty");
            } else {
                this.username = input.getText();
                stage.setScene(new Scene(createChat()));
                startRefreshInterval();
            }
        });
        okButton.setPrefWidth(90);
        HBox hBox = new HBox(10, input, okButton);
        hBox.setPrefSize(450, 100);
        hBox.setAlignment(Pos.CENTER);

        vBoxRoot.getChildren().addAll(hBoxText, hBox);
        return vBoxRoot;
    }

    private void startRefreshInterval() {
        new Thread(() -> {
            while (!isWindowClosed) {
                try {
                    Thread.sleep(500);
                    refresh();
                } catch (InterruptedException e) {
                    // try catch konieczny ze wzgledu na Thread.sleep
                }
            }
        }).start();
    }

    private synchronized Parent createChat() {
        messages = new ListView<>();
        innerMessagesList = FXCollections.observableList(new ArrayList<>());
        messages.setItems(innerMessagesList);
        messages.setPrefHeight(550);
        TextField input = new TextField();
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                chatServer.sendMessage(new Message(username, input.getText()));
                input.setText("");
            }
        });
        VBox messagesContainer = new VBox(20, messages, input);
        messagesContainer.setPrefSize(600, 600);

        users = new ListView<>();
        innerUsersList = FXCollections.observableList(new ArrayList<>());
        users.setItems(innerUsersList);
        users.setPrefSize(150, 600);
        HBox root = new HBox(10, messagesContainer, users);
        root.setPrefSize(760, 600);
        return root;
    }

    private void refresh() {
        chatData = chatServer.refresh();
        refreshUsersList();
        refreshMessagesHistory();
    }

    private void refreshMessagesHistory() {
        NavigableSet<Message> messages = chatData.getMessages();
        List<String> messagesL = messages.stream().map(m -> m.getMessageFullContent()).collect(Collectors.toList());
        chatData.getMessages().forEach(msg -> System.out.println(msg.getMessageFullContent()));
        // metoda startRefereshInterval() odpalana jest w nowym wątku. GUI JavaFX moze być zmieniane tylko z poziomu
        // głownego wątku (czyli watku z metody main)
        Platform.runLater(() -> {
            innerMessagesList.clear();
            innerMessagesList.addAll(messagesL);
            this.messages.refresh();
        });
    }

    private void refreshUsersList() {
        chatData.getUsers().forEach(usr -> System.out.println(usr));
        Platform.runLater(() -> {
            innerUsersList.clear();
            innerUsersList.addAll(chatData.getUsers());
            users.refresh();
        });
    }

}
