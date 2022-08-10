package com.example.javafxtest;
import javafx.scene.paint.Color;

/**
 * A class used to store game results data and that information is accessed in Game.java to display results
 * of the game
 *
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






