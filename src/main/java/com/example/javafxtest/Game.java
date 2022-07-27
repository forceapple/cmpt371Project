package com.example.javafxtest;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import networking.client.NetworkClient;

public class Game {
    Canvas[] canvases;


    private NetworkClient networkClient;
    Game(Stage primaryStage, NetworkClient client) {
        canvases = new Canvas[64];

        this.networkClient = client;
        GridPane grid = new GridPane();

        for(int j=0; j<64; j++){
            Canvas canvas = new Canvas(100, 100);
            canvases[j] = canvas;
            canvas.setId(Integer.toString(j));
        }
        int count = 0;
        for (int i= 0; i< 8 ; i++){
            for (int j= 0; j< 8 ; j++){
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


        StackPane root = new StackPane();
        root.getChildren().add(grid);
        Scene scene = new Scene(root, grid.getPrefWidth(), grid.getPrefHeight());
        primaryStage.setScene(scene);
        primaryStage.show();

    }


    private void makeCanvasDrawable(GraphicsContext graphicsContext, Canvas canvas) {
        int thisCanvasId = Integer.parseInt(canvas.getId());
        AnimationTimer animationTimer = getAnimationTimer(canvas);

        animationTimer.start();

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                new EventHandler<MouseEvent>(){

                    @Override
                    public void handle(MouseEvent event) {

                        // If the canvas isn't drawable then something should indicate this to the player
                        if(!networkClient.selectCanvasForDrawing(thisCanvasId)) {
                            return;
                        }
                        if(!networkClient.getIsLockedByID(thisCanvasId)){
                            graphicsContext.setStroke(networkClient.clientColor);
                            graphicsContext.beginPath();
                            graphicsContext.moveTo(event.getX(), event.getY());
                            graphicsContext.stroke();

                            networkClient.sendDrawing(event.getX(), event.getY());
                        }
                    }
                });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
                new EventHandler<MouseEvent>(){
                    @Override
                    public void handle(MouseEvent event) {

                        if(networkClient.currentCanvasID != thisCanvasId) {
                            return;
                        }
                        if(!networkClient.getIsLockedByID(thisCanvasId)) {
                            graphicsContext.lineTo(event.getX(), event.getY());
                            graphicsContext.stroke();

                            networkClient.sendDrawing(event.getX(), event.getY());
                        }
                    }
                });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED,
                new EventHandler<MouseEvent>(){
                    @Override
                    public void handle(MouseEvent event) {
                        double fillPercentage = computeFillPercentage(graphicsContext);
                        if(!networkClient.getIsLockedByID(thisCanvasId)) {
                            if(fillPercentage > 50) {
                                networkClient.sendLockCanvasRequest(thisCanvasId);
                                graphicsContext.setFill(networkClient.clientColor);
                                graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                                networkClient.sendOwnCanvasbyID(thisCanvasId, networkClient.clientColor);
                            }
                            else {
                                // CLEAR CANVAS
                                graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                                networkClient.sendClearCanvasByID(thisCanvasId);
                            }
                        }




                        System.out.println("filled %: " + fillPercentage);
                        networkClient.releaseCanvas();
                    }
                });
    }

    private AnimationTimer getAnimationTimer(Canvas canvas) {
        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                // Only try to draw if the queue has something to draw
                if(networkClient.networkInputs.areInputsAvailable()) {
                    DrawInfo info = networkClient.networkInputs.getNextInput();
                    GraphicsContext drawContext = canvases[info.getCanvasID()].getGraphicsContext2D();
                    if(info.isClearCanvas()){
                        drawContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                        ((StackPane)canvases[info.getCanvasID()].getParent()).setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(3) )));
                        drawContext.setStroke(info.getColor());

                    }

                    if(info.isOwnCanvas()){
                        drawContext.setFill(info.getColor());
                        drawContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                        ((StackPane)canvases[info.getCanvasID()].getParent()).setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(3) )));
                        drawContext.setStroke(info.getColor());

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

}
