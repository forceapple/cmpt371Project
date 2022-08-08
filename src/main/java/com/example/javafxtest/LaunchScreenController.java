package com.example.javafxtest;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import networking.client.NetworkClient;

import java.io.IOException;
import java.net.ConnectException;


public class LaunchScreenController {

    @FXML
    private TextField hostName;
    @FXML
    private TextField portText;
    @FXML
    private Text connectMessage;
    @FXML
    private GridPane colorGrid;
    @FXML
    private Text colorMessage;
    @FXML
    private Canvas selectedColorCanvas;

    @FXML
    private TextField redTextField;
    @FXML
    private TextField greenTextField;
    @FXML
    private TextField blueTextField;
    @FXML
    private Button registerColorButton;
    @FXML
    private Button connectButton;
    @FXML
    private Button startGameButton;


    private NetworkClient networkClient;

    private final Color colours[] = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,

            Color.YELLOW,
            Color.GREY,
            Color.LAWNGREEN,

            Color.CYAN,
            Color.ORANGE,
            Color.INDIGO
    };

    @FXML
    public void initialize() {
        portText.textProperty().addListener(new NumericInput(portText));
        redTextField.textProperty().addListener(new DecimalInput(redTextField));
        greenTextField.textProperty().addListener(new DecimalInput(greenTextField));
        blueTextField.textProperty().addListener(new DecimalInput(blueTextField));

        for(int x = 0; x < 3; x++) {
            for(int y = 0; y < 3; y++) {

                Canvas canvas = new Canvas(25, 25);
                colorCanvas(canvas, 3*x + y);
                StackPane pane = new StackPane(canvas);

                canvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        canvasClicked(canvas);
                    }
                });

                pane.setMaxSize(canvas.getWidth() + 3, canvas.getHeight() + 3);
                pane.setStyle("-fx-background-color: black");
                colorGrid.add(pane, x, y);
            }

        }
    }

    private void canvasClicked(Canvas canvas) {
        colorMessage.setVisible(false); // Hide message once canvas clicked
        GraphicsContext gc = selectedColorCanvas.getGraphicsContext2D();
        Paint canvasPaint = canvas.getGraphicsContext2D().getFill();
        gc.setFill(canvasPaint);
        gc.fillRect(0, 0, selectedColorCanvas.getWidth(), selectedColorCanvas.getHeight());
        redTextField.setText(Double.toString(((Color)canvasPaint).getRed()));
        blueTextField.setText(Double.toString(((Color)canvasPaint).getBlue()));
        greenTextField.setText(Double.toString(((Color)canvasPaint).getGreen()));
        registerColorButton.setDisable(false);
    }
    private void colorCanvas(Canvas canvas, int colorIndex) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(colours[colorIndex]);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    @FXML
    private void connectButtonClicked(MouseEvent mouseEvent) {
        // The default host and port are the prompt text
        String host = hostName.getText().isBlank() ? hostName.getPromptText() : hostName.getText();
        String port = portText.getText().isBlank() ? portText.getPromptText() : portText.getText();

        // Attempt to connect in another thread to prevent UI from freezing during the processes
        Thread connectThread = new Thread(() -> {
            try {
                connectMessage.setFill(Color.BLACK);
                connectMessage.setText("Connecting...");
                connectMessage.setVisible(true);

                networkClient = new NetworkClient(host, port);

                // Only reaches here if no exceptions
                connectMessage.setText("Connection Successful");
                connectButton.setDisable(true); // Don't allow connecting multiple times
                //registerColorButton.setDisable(false); // Now allow registering colours
            }
            catch(ConnectException e) {
                connectMessage.setFill(Color.RED);
                connectMessage.setText("Unable to connect");
            }
            catch(IllegalArgumentException e) {
                connectMessage.setFill(Color.RED);
                connectMessage.setText("Invalid hostname or port");
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        });
        connectThread.start();

    }

    @FXML
    private void registerColorButtonClicked(MouseEvent e) {
        boolean success = networkClient.registerColor((Color)selectedColorCanvas.getGraphicsContext2D().getFill());
        if(success) {
            colorMessage.setFill(Color.BLACK);
            colorMessage.setText("Colour Registered     "); // Extra spaces prevent UI from moving when text changes
            startGameButton.setDisable(false);
        }
        else {
            colorMessage.setFill(Color.RED);
            colorMessage.setText("Colour already in use");
        }

        colorMessage.setVisible(true);
    }

    @FXML
    private void startGameButtonClicked(MouseEvent e) {
        networkClient.startClient();
        Stage stage = new Stage();
        Game game = new Game(stage, networkClient);
        ((Stage)((Node)e.getSource()).getScene().getWindow()).close(); // Get the stage and close it
    }

    private void displayCustomColor() {
        try {
            double red = Double.parseDouble(redTextField.getText());
            double green = Double.parseDouble(greenTextField.getText());
            double blue = Double.parseDouble(blueTextField.getText());

            // Only get here if no exception

            if(red <= 1 && green <= 1 && blue <= 1) {
                Color color = new Color(red, green, blue, 1);
                GraphicsContext gc = selectedColorCanvas.getGraphicsContext2D();
                gc.setFill(color);
                gc.fillRect(0, 0, selectedColorCanvas.getWidth(), selectedColorCanvas.getHeight());
                registerColorButton.setDisable(false);
            }
        }
        catch(NumberFormatException ignored) {
            // Ignore error
        }


    }

    @FXML
    // Used by the root element to deselect the focus from a text field
    private void deselectFocus(MouseEvent e) {
        ((AnchorPane)e.getSource()).requestFocus();
    }

    private class NumericInput implements ChangeListener<String> {

        private final TextField textField;

        NumericInput(TextField textField) {
            this.textField = textField;
        }

        @Override
        public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
            if(!newValue.matches("\\d*")) {
                textField.setText(oldValue);
            }
            else {
                displayCustomColor();
            }
        }
    }

    private class DecimalInput implements ChangeListener<String> {

        private final TextField textField;

        DecimalInput(TextField textField) {
            this.textField = textField;
        }

        @Override
        public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
            if(!newValue.matches("\\d*(\\.\\d*)?")) {
                textField.setText(oldValue);
            }
            else {
                displayCustomColor();
            }
        }
    }

}


