package com.example.javafxtest;

import javafx.application.Application;
import javafx.stage.Stage;
import networking.client.NetworkClient;



public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }
//    private PixelWriter pWriter;

    private NetworkClient client;
    private NetworkClient setupClient() {
        client = new NetworkClient();

        client.startClient();
        return client;
    }

    @Override
    public void start(Stage primaryStage) {

        NetworkClient client = setupClient();
        Game newGame = new Game(primaryStage, client);
    }


}