package com.example.javafxtest;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.paint.Color;

public class LobbyPlayer {

    private final Color playerColor;
    private final String name;

    private boolean playerReady;

    private final BooleanProperty playerReadyProperty = new SimpleBooleanProperty();
    private boolean playerIsUser;

    public LobbyPlayer(Color color, String name) {
        playerReadyProperty.set(false);
        playerReady = false;
        playerIsUser = false;
        this.playerColor = color;
        this.name = name;
    }

    public Color getPlayerColor() {
        return playerColor;
    }

    public String getPlayerName() {
        return name;
    }

    public void setPlayerReady(boolean ready) {
        playerReady = ready;
        playerReadyProperty.set(ready);
    }

    public boolean isPlayerReady() {
        return playerReady;
    }

    public BooleanProperty getPlayerReadyProperty() {
        return playerReadyProperty;
    }

    /**
     * Set if this LobbyPlayer represents the actual user.
     * i.e. The player who is "you"
     * @param isUser The value that should be set
     */
    public void setPlayerIsUser(boolean isUser) {
        playerIsUser = isUser;
    }

    /**
     * Checks if this LobbyPlayer represents the actual user.
     * @return True if yes, false otherwise
     */
    public boolean isPlayerUser() {
        return playerIsUser;
    }

}


