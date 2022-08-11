package com.example.javafxtest;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import networking.client.NetworkClient;

public class LobbyController {

    @FXML
    private ListView<LobbyPlayer> lobbyMembersListView;
    @FXML
    private Text gameStartText;

    private NetworkClient networkClient;

    private boolean readyValue = false;
    LobbyPlayer user;

    private int countdownSeconds = 3;

    public void setNetworkClient(NetworkClient client) {
        networkClient = client;
        user = new LobbyPlayer(networkClient.clientColor, "Player"); // Other names can be implemented later
        user.setPlayerIsUser(true);
        networkClient.startLobby(new LobbyControllerCallback(), user);

        // This is a rather inelegant way of doing this
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                if(lobbyMembersListView.getItems().size() > 0 && lobbyMembersListView.getItems().get(0).isPlayerReady() != readyValue) {
                    readyValue = !readyValue;
                    networkClient.setPlayerReady(user, readyValue);
                }
            }
        };

        timer.start();
    }


    @FXML
    public void initialize() {
        lobbyMembersListView.setCellFactory(lobbyPlayerCellListView -> new LobbyPlayerCell());
    }

    private void startGame() {
        Stage gameStage = new Stage();
        Game game = new Game(gameStage, networkClient);

        Stage stage = (Stage) lobbyMembersListView.getScene().getWindow();
        stage.close();
    }

    private void startCountdown() {
        gameStartText.setVisible(true);
        lobbyMembersListView.addEventFilter(MouseEvent.ANY, Event::consume);

        Timeline countdownTimeline = new Timeline();
        countdownTimeline.setCycleCount(countdownSeconds);
        countdownTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), actionEvent -> {
            countdownSeconds--;
            gameStartText.setText("Game Starting in: " + countdownSeconds);
        }));

        countdownTimeline.setOnFinished(actionEvent -> startGame());
        countdownTimeline.playFromStart();
    }


    public class LobbyControllerCallback implements com.example.javafxtest.LobbyControllerCallback {

        @Override
        public void addPlayer(LobbyPlayer player) {
            Platform.runLater(() -> lobbyMembersListView.getItems().add(player));
        }

        @Override
        public void removePlayer(int index) {
            Platform.runLater(() -> lobbyMembersListView.getItems().remove(index));
        }

        @Override
        public void setReady(int index, boolean isReady) {
            Platform.runLater(() -> lobbyMembersListView.getItems().get(index).setPlayerReady(isReady));
        }

        @Override
        public void startGameCountdown() {
            Platform.runLater(LobbyController.this::startCountdown);
        }
    }



}
