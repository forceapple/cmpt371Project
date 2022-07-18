package com.example.javafxtest;

import javafx.application.Application;
import javafx.scene.paint.Color;
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

        NetworkClient client = setupClient();
        Game newGame = new Game(primaryStage, client);
    }


}