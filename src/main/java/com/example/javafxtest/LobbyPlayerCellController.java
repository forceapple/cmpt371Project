package com.example.javafxtest;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.RadioButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.io.IOException;

public class LobbyPlayerCellController {

    // These FXML fields are actually assigned when the Controller is loader
    @FXML
    private Canvas playerColorCanvas;
    @FXML
    private Text playerNameText;
    @FXML
    private RadioButton playerReadyButton;
    private LobbyPlayer lobbyPlayer;
    private final AnchorPane rootPane;

    private final EventHandler<MouseEvent> consumeEvent = Event::consume;
    private final ChangeListener<Boolean> changeListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old, Boolean newValue) {
            lobbyPlayer.setPlayerReady(newValue);
        }
    };


    public LobbyPlayerCellController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LobbyPlayerListCell.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLobbyPlayer(LobbyPlayer player) {

        playerReadyButton.selectedProperty().unbind();
        playerReadyButton.selectedProperty().removeListener(changeListener);
        playerReadyButton.removeEventFilter(MouseEvent.ANY, consumeEvent);

        lobbyPlayer = player;
        playerColorCanvas.getGraphicsContext2D().setFill(player.getPlayerColor());
        playerColorCanvas.getGraphicsContext2D().fillRect(0, 0, playerColorCanvas.getWidth(), playerColorCanvas.getHeight());
        playerNameText.setText(player.getPlayerName());

        // If the player is the user, then allow changing the ready box and when clicked update the value in lobbyPlayer
        if(player.isPlayerUser()) {
            playerReadyButton.selectedProperty().addListener(changeListener);

        }
        // If the player is not the user, then do not allow changing the ready box
        else {
            playerReadyButton.addEventFilter(MouseEvent.ANY, consumeEvent);
            playerReadyButton.selectedProperty().bind(lobbyPlayer.getPlayerReadyProperty());
        }
    }

    public Node getView() {
        return rootPane;
    }

}
