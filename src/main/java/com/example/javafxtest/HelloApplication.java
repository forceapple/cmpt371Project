package com.example.javafxtest;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.stage.Stage;
import networking.client.NetworkClient;
import networking.client.NetworkObserver;

;

public class HelloApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }
    double gridSize = 50;
    private GraphicsContext graphicsContext;
    private PixelWriter pWriter;

    private NetworkClient client;
    private void setupClient() {
        client = new NetworkClient();

        client.addObserver(new NetworkObserver() {
            @Override
            public void messageReceived(String message) {
                DrawInfo info = DrawInfo.fromJson(message);
                pWriter.setColor((int)Math.round(info.getX()), (int)Math.round(info.getY()), Color.BLUE);
//                graphicsContext.beginPath();
//                graphicsContext.moveTo(info.getX(), info.getY());
//                graphicsContext.stroke();
            }
        });

        client.startClient();
    }

    @Override
    public void start(Stage primaryStage) {

        setupClient();

        Canvas canvas = new Canvas(500, 500);
        graphicsContext = canvas.getGraphicsContext2D();
        pWriter = graphicsContext.getPixelWriter();
        initDraw(graphicsContext);

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                new EventHandler<MouseEvent>(){

                    @Override
                    public void handle(MouseEvent event) {
                        pWriter.setColor((int)Math.round(event.getX()), (int)Math.round(event.getY()), Color.BLUE);

//                        graphicsContext.beginPath();
//                        graphicsContext.moveTo(event.getX(), event.getY());
//                        graphicsContext.stroke();

                        client.sendMessage(DrawInfo.toJson(new DrawInfo(event.getX(), event.getY())));
                    }
                });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
                new EventHandler<MouseEvent>(){

                    @Override
                    public void handle(MouseEvent event) {
                        pWriter.setColor((int)Math.round(event.getX()), (int)Math.round(event.getY()), Color.BLUE);
//                        graphicsContext.lineTo(event.getX(), event.getY());
//                        graphicsContext.stroke();

                        client.sendMessage(DrawInfo.toJson(new DrawInfo(event.getX(), event.getY())));
                    }
                });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED,
                new EventHandler<MouseEvent>(){

                    @Override
                    public void handle(MouseEvent event) {

                    }
                });

        StackPane root = new StackPane();
        root.getChildren().add(canvas);
        Scene scene = new Scene(root, 800, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        scene.setFill(createGridPattern());
    }



    private void initDraw(GraphicsContext gc){
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
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1);



    }

    public ImagePattern createGridPattern() {

        double w = gridSize;
        double h = gridSize;

        Canvas canvas = new Canvas(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setStroke(Color.BLACK);
        gc.setFill(Color.LIGHTGRAY.deriveColor(1, 1, 1, 0.2));
        gc.fillRect(0, 0, w, h);
        gc.strokeRect(0, 0, w, h);

        Image image = canvas.snapshot(new SnapshotParameters(), null);
        ImagePattern pattern = new ImagePattern(image, 0, 0, w, h, false);

        return pattern;

    }
}