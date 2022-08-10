package com.example.javafxtest;
import javafx.scene.paint.Color;

/**
 * A class used to send end game Results to the client's
 * Used for communication about game end Results between server and game
 */
public class GameResults{

    private final int winnerScore;
    private final Color winnerColor;

    public GameResults(int winnerScore, Color winnerColor) {
        this.winnerScore = winnerScore;
        this.winnerColor = winnerColor;
    }

    public int getWinnerScore() {
        return winnerScore;
    }


    public Color getWinnerColor(){
        return winnerColor;
    }
}






