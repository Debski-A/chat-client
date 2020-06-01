package com.main;

import com.caucho.hessian.client.HessianProxyFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;
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
    private XmlRpcClient chatServerXmlRpc;
    private String username;
    private volatile boolean isWindowClosed = false;
    private volatile boolean isXmlRpcEnabled = false;
    private ChatDataSingleton chatDataSingleton;
    private ListView<String> users;
    private ObservableList<String> innerUsersList;
    private ListView<String> messages;
    private ObservableList<String> innerMessagesList;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws MalformedURLException, XmlRpcException {
        this.stage = stage;
        String serverEndpoint = "http://localhost:8080/chat-server/chat";
        HessianProxyFactory hessianProxyFactory = new HessianProxyFactory();
        chatServer = (ChatServerApi) hessianProxyFactory.create(ChatServerApi.class, serverEndpoint);
        stage.setOnCloseRequest(e -> {
            isWindowClosed = true;
            chatServer.userLeft(username);
        });

        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setEnabledForExtensions(true);
        config.setServerURL(new URL("http://localhost:8080/chat-server/chat-xml-rpc"));
        chatServerXmlRpc = new XmlRpcClient();
        chatServerXmlRpc.setConfig(config);

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
                    if (isXmlRpcEnabled) {
                        refreshXmlRpc();
                    } else {
                        refresh();
                    }
                } catch (InterruptedException | XmlRpcException e) {
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
                if (isXmlRpcEnabled) {
                    Object[] params = new Object[]{new Message(username, input.getText())};
                    try {
                        chatServerXmlRpc.execute("ChatServerXmlRpc.sendMessage", params);
                    } catch (XmlRpcException xmlRpcException) {
                        xmlRpcException.printStackTrace();
                    }
                } else {
                    chatServer.sendMessage(new Message(username, input.getText()));
                }
                input.setText("");
            }
        });
        VBox messagesContainer = new VBox(20, messages, input);
        messagesContainer.setPrefSize(600, 600);

        users = new ListView<>();
        innerUsersList = FXCollections.observableList(new ArrayList<>());
        users.setItems(innerUsersList);
        users.setPrefSize(150, 550);
        final ToggleGroup group = new ToggleGroup();
        RadioButton hessianRadio = new RadioButton("Hessian");
        hessianRadio.setToggleGroup(group);
        hessianRadio.setSelected(true);
        RadioButton xmlRpcRadio = new RadioButton("XML-RPC");
        xmlRpcRadio.setToggleGroup(group);
        // w zalezonosci od zaznaczonego Radio Buttona komunikacja z serwerem bedzie sie odbywac przez Hessian
        // lub XML RPC
        hessianRadio.setOnMouseClicked(e -> {
            if (hessianRadio.isSelected()) {
                isXmlRpcEnabled = false;
            }
        });
        xmlRpcRadio.setOnMouseClicked(e -> {
            if (xmlRpcRadio.isSelected()) {
                isXmlRpcEnabled = true;
            }
        });

        VBox usersAndSwitchContainer = new VBox(20, users, hessianRadio, xmlRpcRadio);
        messagesContainer.setPrefSize(600, 600);

        HBox root = new HBox(10, messagesContainer, usersAndSwitchContainer);
        root.setPrefSize(760, 600);
        return root;
    }

    private void refresh() {
        chatDataSingleton = chatServer.refresh();
        refreshUsersList();
        refreshMessagesHistory();
    }

    private void refreshXmlRpc() throws XmlRpcException {
        chatDataSingleton = (ChatDataSingleton) chatServerXmlRpc.execute("ChatServerXmlRpc.refresh", new Object[0]);
        refreshUsersList();
        refreshMessagesHistory();
    }

    private void refreshMessagesHistory() {
        NavigableSet<Message> messages = chatDataSingleton.getMessages();
        List<String> messagesL = messages.stream().map(m -> m.getMessageFullContent()).collect(Collectors.toList());
        //metoda startRefereshInterval() odpalana jest w nowym wątku. GUI JavaFX moze być zmieniane tylko z poziomu
        //głownego wątku JavaFX (czyli watku z metody main). Rozwiazaniem jest metoda Platform.runLater, ktora
        //wstrzeliwjue sie w "wolny czas" glownego watku i odpala na nim swoj kawalek ponizszego kodu,ktory zmienia GUI
        Platform.runLater(() -> {
            innerMessagesList.clear();
            innerMessagesList.addAll(messagesL);
        });
    }

    private void refreshUsersList() {
        // jak wyzej
        Platform.runLater(() -> {
            innerUsersList.clear();
            innerUsersList.addAll(chatDataSingleton.getUsers());
        });
    }

}
