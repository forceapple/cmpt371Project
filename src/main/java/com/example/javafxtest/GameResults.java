package com.example.javafxtest;
import javafx.scene.paint.Color;

/**
 * A class used to send end game Results to the client's
 * Used for communication about game end Results between server and game
 */
public class GameResults{

    private final boolean isGameOver;
    private final int winnerScore;
    private final String gameResultMsg;
    private final Color winnerColor;

    public GameResults(boolean isGameOver, int winnerScore, String gameResultMsg, Color winnerColor) {
        this.isGameOver = isGameOver;
        this.winnerScore = winnerScore;
        this.gameResultMsg = gameResultMsg;
        this.winnerColor = winnerColor;
    }

    public int getWinnerScore() {
        return winnerScore;
    }

    public String getResultMsg() {
        return gameResultMsg;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public Color getWinnerColor(){
        return winnerColor;
    }
}






