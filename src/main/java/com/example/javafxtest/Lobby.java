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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import networking.client.NetworkClient;

import java.util.ArrayList;
import java.util.List;

public class Lobby {

    private NetworkClient networkClient;

    private List<Color> colors = new ArrayList<>();
    private ListView<Rectangle> listView = new ListView<>();

    private Color hostColorLobby;
    Lobby(Stage primaryStage, NetworkClient client) {
        this.networkClient = client;
        hostColorLobby = client.clientColor;


        //setting up the GridPane
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(30, 150, 30, 150));
        grid.setVgap(8);
        grid.setHgap(10);

        //label for the lobby
        Label labelLobby = new Label("LOBBY");
        GridPane.setConstraints(labelLobby, 2, 0);
        GridPane.setHalignment(labelLobby, HPos.CENTER);

        //hostColor = client.clientColor;
        Rectangle rectangle = new Rectangle();
        rectangle.setWidth(30);
        rectangle.setHeight(30);
        rectangle.setFill(hostColorLobby);
        GridPane.setConstraints(rectangle, 1,1);

        Label labelHost = new Label("is the host and can start the game");
        GridPane.setConstraints(labelHost, 2,1);

        //setting up the listview
        addPlayerColor(getListView(),client.clientColor);
        colors.add(client.clientColor);
        GridPane.setConstraints(getListView(), 2, 2);

        //setting up the button
        Button buttonStartGame = new Button("Start Game");
        GridPane.setConstraints(buttonStartGame, 2, 3);
        GridPane.setHalignment(buttonStartGame, HPos.CENTER);
        buttonStartGame.setOnAction(e -> {
            Stage stage = new Stage();
            Game game = new Game(stage, client);
            ((Stage)((Node)e.getSource()).getScene().getWindow()).close();
        });

        grid.getChildren().addAll(labelLobby, rectangle, labelHost, getListView(), buttonStartGame);
        Scene scene = new Scene(grid, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Deny & Conquer");
        primaryStage.show();

        AnimationTimer animationTimer = getAnimationTimer();
        animationTimer.start();
        networkClient.sendJoinGame();

    }

    private void addPlayerColor(ListView<Rectangle> listView, Color color) {
        if(colors.contains(color))
            return;
        Rectangle rectangle = new Rectangle();
        rectangle.setHeight(30);
        rectangle.setWidth(30);
        rectangle.setFill(color);
        listView.getItems().add(rectangle);
        colors.add(color);
    }
    private ListView<Rectangle> getListView() {
        return listView;
    }

    private AnimationTimer getAnimationTimer() {
        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                // Only try to draw if the queue has something to draw
                while(networkClient.networkInputs.areInputsLobbyAvailable()){
                    Color color = networkClient.networkInputs.getNextPlayerInput();
                    addPlayerColor(getListView(),color);
                }
            }
        };
        return animationTimer;
    }
}
