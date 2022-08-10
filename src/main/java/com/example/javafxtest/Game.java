package com.example.javafxtest;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import networking.client.NetworkClient;

public class Game {
    private Canvas[] canvases;
    private NetworkClient networkClient;

    private static Label scoresLabel;
    private static Label playerColorLabel;

    private static Rectangle clientColorRect;
    private int score = 0;

    Game(Stage primaryStage, NetworkClient client) {
        canvases = new Canvas[8];

        this.networkClient = client;
        GridPane grid = new GridPane();

        for(int j=0; j<8; j++){
            Canvas canvas = new Canvas(70, 70);
            canvases[j] = canvas;
            canvas.setId(Integer.toString(j));
        }
        int count = 0;
        for (int i= 0; i< 4 ; i++){
            for (int j= 0; j< 2; j++){
                // Put the canvases inside a StackPane and give the StackPane a border
                StackPane pane = new StackPane(canvases[count]);
                pane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(3) )));

                grid.add(pane, i, j,1,1);
                final GraphicsContext graphicsContext = canvases[count].getGraphicsContext2D();
                makeCanvasDrawable(graphicsContext, canvases[count]);
                count++;
            }
        }

        grid.setHgap(10);
        grid.setVgap(5);

        grid.setPadding(new Insets(10,10,10,10));

        playerColorLabel = new Label( "Your Color: " );
        playerColorLabel.setFont(new Font("Arial", 20));
        playerColorLabel.setStyle("-fx-font-weight: bold");
        clientColorRect = new Rectangle();
        clientColorRect.setWidth(30);
        clientColorRect.setHeight(30);
        clientColorRect.setFill(networkClient.clientColor);

        scoresLabel = new Label( "Score Rules: You will get 10 points for each coloured Box" );
        scoresLabel.setFont(new Font("Arial", 20));
        scoresLabel.setAlignment(Pos.CENTER);
        scoresLabel.setStyle("-fx-background-color: grey; -fx-font-weight: bold ");
        GridPane root = new GridPane();
        root.setPadding(new Insets(10,10,10,10));
        root.setHgap(10);
        root.setVgap(10);

        GridPane.setConstraints(playerColorLabel, 0,0);
        GridPane.setConstraints(clientColorRect, 0,0);
        GridPane.setConstraints(scoresLabel, 0, 1);
        GridPane.setConstraints(grid, 0, 2);

        GridPane.setMargin(clientColorRect, new Insets(5,5,5,135));
        root.getChildren().addAll(playerColorLabel,clientColorRect, scoresLabel, grid);
        Scene scene = new Scene(root, grid.getPrefWidth(), grid.getPrefHeight());
        primaryStage.setScene(scene);
        primaryStage.show();

        AnimationTimer animationTimer = getAnimationTimer();
        animationTimer.start();




    }


    private void makeCanvasDrawable(GraphicsContext graphicsContext, Canvas canvas) {
        int thisCanvasId = Integer.parseInt(canvas.getId());

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                new EventHandler<MouseEvent>(){
                    @Override
                    public void handle(MouseEvent event) {

                        // If the canvas isn't drawable then something should indicate this to the player
                        if(!networkClient.selectCanvasForDrawing(thisCanvasId)) {
                            return;
                        }

                        graphicsContext.setStroke(networkClient.clientColor);
                        graphicsContext.beginPath();
                        graphicsContext.moveTo(event.getX(), event.getY());
                        graphicsContext.stroke();

                        networkClient.sendDrawing(event.getX(), event.getY());
                    }
                });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
                new EventHandler<MouseEvent>(){
                    @Override
                    public void handle(MouseEvent event) {

                        if(networkClient.currentCanvasID != thisCanvasId) {
                            return;
                        }
                        graphicsContext.lineTo(event.getX(), event.getY());
                        graphicsContext.stroke();

                        networkClient.sendDrawing(event.getX(), event.getY());
                    }
                });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED,
                new EventHandler<MouseEvent>(){
                    @Override
                    public void handle(MouseEvent event) {
                        double fillPercentage = computeFillPercentage(graphicsContext);

                        //prevent mouse release to clear the canvas.
                        if(networkClient.currentCanvasID != thisCanvasId) {
                            return;
                        }

                        if(fillPercentage > 50) {
                            networkClient.sendLockCanvasRequest();
                            graphicsContext.setFill(networkClient.clientColor);
                            graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                            score += 10;
                            scoresLabel.setText("Your Score:   " + score);//displaying score on clients' canvas
                            networkClient.sendOwnCanvas();
                            networkClient.sendScore(score); // sending scores
                        }
                        else {
                            // CLEAR CANVAS
                            graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                            networkClient.sendClearCanvas();
                        }

                        System.out.println("filled %: " + fillPercentage);
                        networkClient.releaseCanvas();
                    }
                });
    }

    private AnimationTimer getAnimationTimer() {
        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                // Only try to draw if the queue has something to draw
                while(networkClient.networkInputs.areInputsAvailable()){
                    DrawInfo info = networkClient.networkInputs.getNextInput();
                    GraphicsContext drawContext = canvases[info.getCanvasID()].getGraphicsContext2D();
                    Canvas currentCanvas = canvases[info.getCanvasID()];

                    if(info.isClearCanvas()){
                        drawContext.clearRect(0, 0, currentCanvas.getWidth(), currentCanvas.getHeight());
                        ((StackPane)canvases[info.getCanvasID()].getParent()).setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(3) )));
                    }
                    else if(info.isOwnCanvas()){
                        drawContext.setFill(info.getColor());
                        drawContext.fillRect(0, 0, currentCanvas.getWidth(), currentCanvas.getHeight());
                        ((StackPane)canvases[info.getCanvasID()].getParent()).setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(3) )));
                    }
                    else if(info.isPathStart()) {
                        ((StackPane)canvases[info.getCanvasID()].getParent()).setBorder(new Border(new BorderStroke(info.getColor(), BorderStrokeStyle.SOLID, null, new BorderWidths(3) )));
                        drawContext.setStroke(info.getColor());
                        drawContext.beginPath();
                        drawContext.moveTo(info.getX(), info.getY());
                        drawContext.stroke();
                    }

                    else {
                        drawContext.lineTo(info.getX(), info.getY());
                        drawContext.stroke();
                    }
                }

            }
        };
        return animationTimer;
    }


    private double computeFillPercentage(GraphicsContext graphicsContext) {

        // converts canvas cell to a writable image
        WritableImage image = graphicsContext.getCanvas().snapshot(null, null);

        // obtains PixelReader from the snap
        PixelReader pixelReader = image.getPixelReader();

        double snapHeight = image.getHeight();
        double snapWidth = image.getWidth();
        double coloredPixels = 0;
        double totalPixels = (snapHeight * snapWidth);

        String hexColor = String.valueOf(networkClient.clientColor);

        // computes the number of colored pixels
        for (int readY = 0; readY < snapHeight; readY++) {
            for (int readX = 0; readX < snapWidth; readX++) {
                Color color = pixelReader.getColor(readX, readY);

                // checks if a pixel is colored with PenColor
                if (color.toString().equals(hexColor)) {
                    coloredPixels += 1;
                }
            }
        }

        // computes colored area percentage
        double fillPercentage = (coloredPixels / totalPixels) * 100.0;
        return fillPercentage;
    }


    //this method runs only when the game ends, and it updates the UI
    public static class GameEndResults implements Runnable{

        private final GameResults results;
        public GameEndResults(GameResults results) {
            this.results = results;
        }
        @Override
        public void run() {
                    if (results.getWinnerColor().equals(Color.TRANSPARENT)) {
                        playerColorLabel.setText("Game Ended with a Tie");
                        clientColorRect.setFill(results.getWinnerColor());
                        scoresLabel.setText("Tie Score " + results.getWinnerScore());
                    } else {
                        playerColorLabel.setText("Game Winner:");
                        clientColorRect.setFill(results.getWinnerColor());
                        scoresLabel.setText("Winner Score " + results.getWinnerScore());
                    }
            }
    }
}



