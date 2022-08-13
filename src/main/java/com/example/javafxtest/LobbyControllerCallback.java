package com.example.javafxtest;

public interface LobbyControllerCallback {
    void addPlayer(LobbyPlayer player);
    void removePlayer(int index);
    void setReady(int index, boolean isReady);
    void startGameCountdown();
}
