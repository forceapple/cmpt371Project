package com.example.javafxtest;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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
                grid.add(canvases[count], i, j,1,1);
                final GraphicsContext graphicsContext = canvases[count].getGraphicsContext2D();
                makeCanvasDrawable(graphicsContext, canvases[count]);
                initDraw(graphicsContext);
                count++;
            }
        }

        grid.setHgap(10);
        grid.setVgap(5);

        grid.setPadding(new Insets(10,10,10,10));


        StackPane root = new StackPane();
        root.getChildren().add(grid);
        Scene scene = new Scene(root, 900, 850);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private void initDraw(GraphicsContext gc) {
        double canvasWidth = gc.getCanvas().getWidth();
        double canvasHeight = gc.getCanvas().getHeight();

        gc.setFill(Color.LIGHTGRAY);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(5);

        gc.fill();
        gc.strokeRect(
                0,              //x of the upper left corner
                0,              //y of the upper left corner
                canvasWidth,    //width of the rectangle
                canvasHeight);  //height of the rectangle

        gc.setFill(Color.RED);
        gc.setStroke(networkClient.clientColor);
        gc.setLineWidth(1);
    }

    private void makeCanvasDrawable(GraphicsContext graphicsContext, Canvas canvas) {
        PixelWriter pWriter = graphicsContext.getPixelWriter();
        PixelWriter finalPWriter = pWriter;
        int thisCanvasId = Integer.parseInt(canvas.getId());
        // This should be in another function
        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                // Only try to draw if the queue has something to draw
                if(networkClient.networkInputs.areInputsAvailable()) {
                    DrawInfo info = networkClient.networkInputs.getNextInput();
                    PixelWriter pWriter = canvases[info.getCanvasID()].getGraphicsContext2D().getPixelWriter();

                    // Having the rounding math done on the main thread may cause unneeded lag
                    // It may be more efficient to do this rounding in another thread so the values are pre-rounded
                    pWriter.setColor((int)Math.round(info.getX()), (int)Math.round(info.getY()), info.getColor());
                }
            }
        };

        animationTimer.start();

        pWriter = graphicsContext.getPixelWriter();
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                new EventHandler<MouseEvent>(){

                    @Override
                    public void handle(MouseEvent event) {

                        // If the canvas isn't drawable then something should indicate this to the player
                        if(!networkClient.selectCanvasForDrawing(thisCanvasId)) {
                            return;
                        }

                        finalPWriter.setColor((int)Math.round(event.getX()), (int)Math.round(event.getY()), networkClient.clientColor);

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


                        System.out.println("x: " + event.getX() + ", y: " + event.getY());
                        System.out.println(canvas.getId());
                        finalPWriter.setColor((int)Math.round(event.getX()), (int)Math.round(event.getY()), networkClient.clientColor);
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
                        System.out.println("filled %: " + fillPercentage);
                        networkClient.releaseCanvas();


                    }
                });
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
