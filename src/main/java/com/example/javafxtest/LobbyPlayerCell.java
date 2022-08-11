package com.example.javafxtest;

import javafx.scene.Node;
import javafx.scene.control.ListCell;

public class LobbyPlayerCell extends ListCell<LobbyPlayer> {
    private final LobbyPlayerCellController playerCellController = new LobbyPlayerCellController();
    private final Node playerCellView = playerCellController.getView();

    @Override
    protected void updateItem(LobbyPlayer lobbyPlayer, boolean empty) {
        super.updateItem(lobbyPlayer, empty);

        if(empty) {
            setGraphic(null);
        }

        else {
            playerCellController.setLobbyPlayer(lobbyPlayer);
            setGraphic(playerCellView);
        }

    }
}
