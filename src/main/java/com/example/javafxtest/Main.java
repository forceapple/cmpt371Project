package com.example.javafxtest;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import networking.client.NetworkClient;



public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }
//    private PixelWriter pWriter;

    private NetworkClient client;
    private NetworkClient setupClient(String host) {
        client = new NetworkClient(host);


        // Temporary setup for the client colours. This should have a better implementation later
        Color possibleColors[] = {Color.BLUE, Color.RED, Color.GREEN, Color.BLACK};
        for(Color c : possibleColors) {
            if(client.registerColor(c)) {
                break;
            }
        }

        client.startClient();
        return client;
    }

    @Override
    public void start(Stage primaryStage) {
        TextField textField = new TextField();
        Button btn = new Button("Set Server Host");
        btn.setTranslateX(15);
        btn.setTranslateY(125);
        Group root = new Group(textField, btn);
        Scene scene = new Scene(root);
        Stage newWindow = new Stage();
        newWindow.setScene(scene);

        btn.setOnMouseClicked(mouseEvent -> {

            NetworkClient client = setupClient(textField.getText());
            Game newGame = new Game(primaryStage, client);

            newWindow.close();
        });

        newWindow.show();
    }


}