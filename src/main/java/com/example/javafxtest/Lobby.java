package com.example.javafxtest;

import javafx.animation.AnimationTimer;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import networking.client.NetworkClient;

public class Lobby {

    private NetworkClient networkClient;

    private ListView<String> listView;
    Lobby(Stage primaryStage, NetworkClient client) {

        //setting up the GridPane
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(30, 150, 30, 150));
        grid.setVgap(8);
        grid.setHgap(10);

        //label for the lobby
        Label labelLobby = new Label("LOBBY");
        GridPane.setConstraints(labelLobby, 2, 0);
        GridPane.setHalignment(labelLobby, HPos.CENTER);

        //setting up the listview
        ListView<String> listView = new ListView<>();
        String stringColour = client.clientColor.toString();
        listView.getItems().addAll(stringColour);
        networkClient.sendJoinGame();
        listView.setDisable(true);
        GridPane.setConstraints(listView, 2, 1);

        //setting up the button
        Button buttonStartGame = new Button("Start Game");
        GridPane.setConstraints(buttonStartGame, 2, 2);
        GridPane.setHalignment(buttonStartGame, HPos.CENTER);
        buttonStartGame.setOnAction(e -> {
            Stage stage = new Stage();
            Game game = new Game(stage, client);
            ((Stage)((Node)e.getSource()).getScene().getWindow()).close();
        });

        grid.getChildren().addAll(labelLobby, listView, buttonStartGame);
        Scene scene = new Scene(grid, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Deny & Conquer");
        primaryStage.show();


//        AnimationTimer animationTimer = getAnimationTimer();
//        animationTimer.start();

    }


//    private AnimationTimer getAnimationTimer() {
//        AnimationTimer animationTimer = new AnimationTimer() {
//            @Override
//            public void handle(long l) {
//                // Only try to draw if the queue has something to draw
//                while(networkClient.networkInputs.areInputsAvailable()){
//                    DrawInfo info = networkClient.networkInputs.getNextInput();
//
//                    if(info.isGameJoin()){
//                        String string = new String(networkClient.clientColor.toString());
//                        listView.getItems().addAll(string);
//                    }
//                }
//            }
//        };
//        return animationTimer;
//    }
}
