package networking.client;

import com.example.javafxtest.DrawInfo;
import com.example.javafxtest.Game;
import com.example.javafxtest.GameResults;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import networking.NetworkMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The NetworkClient facilitates communication between the local game and the network server.
 * All communication between the two should be done via this class.
 */
public class NetworkClient {

    // The amount of time (in ms) that the game will wait for a response from the server.
    private static final int SERVER_RESPONSE_TIMEOUT = 100;

    private PrintWriter output;
    private BufferedReader input;
    private Socket socket;
    private final List<NetworkObserver> observers;

    public Color clientColor = null;
    public final InputHandler networkInputs;

    public int currentCanvasID;

    // serverResponseBoolSync is only used as a synchronization lock for serverResponseBool
    // This thread will call await() on serverResponseSync, and it will be woken up
    // when there is a server response which is saved in serverResponseBool
    // This is honestly a messy approach. It might be better to change this in the future
    private final Boolean serverResponseBoolSync;

    private final BlockingQueue<Boolean> serverBoolResponseQueue;

    private boolean serverResponseBool;

    private boolean clientRunning = false;
    private boolean firstDraw = false;

    public NetworkClient(String host, String port) throws IOException, IllegalArgumentException {
        serverBoolResponseQueue = new LinkedBlockingQueue<>();
        observers = new ArrayList<>();
        networkInputs = new InputHandler();
        addObserver(networkInputs);
        serverResponseBoolSync = false;
        serverResponseBool = false;
        currentCanvasID = -1;
        socket = new Socket(host, Integer.parseInt(port));
        try {

            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        ClientNetworkThread networkThread = new ClientNetworkThread(input, observers);
        networkThread.setName("Client Network Thread");
        networkThread.setDaemon(true);
        networkThread.start();
    }

    private void addObserver(NetworkObserver obs) {
        this.observers.add(obs);
    }

    private void removeObserver(NetworkObserver obs) {
        this.observers.remove(obs);
    }

    public void startClient() {

        if(clientColor == null) {
            throw new IllegalStateException("Attempting to start the client without registering a colour!");
        }

        clientRunning = true;
    }

    /**
     * Attempts to register a colour with the server. The method will block until a response from the server occurs
     * or the response times out.
     * @param color The colour being registered
     * @return Returns true if the colour is successfully registered and false if the colour is already in use
     */
    public boolean registerColor(Color color) {
        output.println(NetworkMessage.addColorRequestHeader(Integer.toString(color.hashCode())));
        Boolean response;

        try {
            response = serverBoolResponseQueue.poll(SERVER_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
            // Null response means timeout occurred
            if(!Objects.nonNull(response)) {
                throw new RuntimeException("Server timed out in registerColor");
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(response) {
            clientColor = color;
        }

        return response;

//        output.println(NetworkMessage.addColorRequestHeader(Integer.toString(color.hashCode())));
//
//        synchronized(serverResponseBoolSync) {
//            try {
//                serverResponseBoolSync.wait();
//            }
//            catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        if(serverResponseBool) {
//            clientColor = color;
//        }
//
//        return serverResponseBool;
    }

    /**
     * Attempts to select a canvas for drawing. If successful then the canvas will be acquired by client
     * and be unable to be used by any other client until the releaseCanvas function is called.
     * This method blocks until a response from the server is received or if the response timeout occurs.
     * @param canvasID The ID of the canvas
     * @return true on success, false on failure
     */
    public boolean selectCanvasForDrawing(int canvasID) {

        if(!clientRunning) {
            throw new IllegalStateException("Attempting to select canvas without a running client");
        }

        output.println(NetworkMessage.addCanvasRequestHeader(Integer.toString(canvasID)));

        Boolean response;

        try {
            response = serverBoolResponseQueue.poll(SERVER_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
            // Null response means timeout occurred
            if(!Objects.nonNull(response)) {
                throw new RuntimeException("Server timed out in selectCanvasForDrawing");
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(response) {
            currentCanvasID = canvasID;
            firstDraw = true;
        }

        return response;


//        // Should default to false in timeout
//        serverResponseBool = false;
//
//        if(!clientRunning) {
//            throw new IllegalStateException("Attempting to select canvas without a running client");
//        }
//
//        output.println(NetworkMessage.addCanvasRequestHeader(Integer.toString(canvasID)));
//
//        synchronized(serverResponseBoolSync) {
//            try {
//                serverResponseBoolSync.wait(200);
//            }
//            catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        if(serverResponseBool) {
//            currentCanvasID = canvasID;
//            firstDraw = true;
//        }
//
//        return serverResponseBool;
    }

    /**
     * Releases the canvas owned by the client. If no canvases are owned then does nothing
     */
    public void releaseCanvas() {
        if(!clientRunning) {
            throw new IllegalStateException("Attempting to release canvas without a running client");
        }

        output.println(NetworkMessage.generateCanvasReleaseMessage());
        currentCanvasID = -1;
    }

    /**
     * Sends a message to the server indicate that a certain pixel on the registered canvas is drawn
     * The color of the pixel is the registered client color
     * @param x The x coordinate of the pixel being drawn
     * @param y The y coordinate of the pixel being drawn
     */
    public void sendDrawing(double x, double y) {

        if(!clientRunning) {
            throw new IllegalStateException("Attempting to draw without a running client");
        }

        if(clientColor == null) {
            throw new IllegalStateException("Attempting to draw without registering a color");
        }
        if(currentCanvasID == -1) {
            throw new IllegalStateException("Attempting to draw without registering a canvas");
        }

        DrawInfo draw = new DrawInfo(x, y, currentCanvasID, clientColor, firstDraw, false, false);
        output.println(NetworkMessage.generateDrawMessage(draw));

        firstDraw = false;
    }
    /**
     * Sends a message to the server indicate the current canvas needs to be locked for other clients
     */
    public void sendLockCanvasRequest() {

        if(!clientRunning) {
            throw new IllegalStateException("Attempting to lock canvas without a running client");
        }

        if(clientColor == null) {
            throw new IllegalStateException("Attempting to lock canvas without registering a color");
        }
        if(currentCanvasID == -1) {
            throw new IllegalStateException("Attempting to lock canvas without registering a canvas");
        }
        String stringCanvasID = Integer.toString(currentCanvasID);
        output.println(NetworkMessage.addCanvasLockRequestHeader(stringCanvasID));
    }
    /**
     * Sends a message to the server indicate the current canvas needs to be cleared for other clients
     */
    public void sendClearCanvas() {

        if(!clientRunning) {
            throw new IllegalStateException("Attempting to clear canvas without a running client");
        }

        if(clientColor == null) {
            throw new IllegalStateException("Attempting to clear canvas without registering a color");
        }
        if(currentCanvasID == -1) {
            throw new IllegalStateException("Attempting to clear canvas without registering a canvas");
        }
        String stringCanvasID = Integer.toString(currentCanvasID);
        output.println(NetworkMessage.addCanvasClearRequestHeader(stringCanvasID));
    }

    public void sendOwnCanvas() {

        if(!clientRunning) {
            throw new IllegalStateException("Attempting to own canvas without a running client");
        }

        if(clientColor == null) {
            throw new IllegalStateException("Attempting to own canvas without registering a color");
        }
        if(currentCanvasID == -1) {
            throw new IllegalStateException("Attempting to own canvas without registering a canvas");
        }
        String stringCanvasID = Integer.toString(currentCanvasID);
        output.println(NetworkMessage.addCanvasOwnRequestHeader(stringCanvasID, clientColor));
    }

    /**
     * Sends a score of clients to the server
     */
    public void sendScore(int score) {

        if(!clientRunning) {
            throw new IllegalStateException("Attempting to send score without a running client");
        }

        if(clientColor == null) {
            throw new IllegalStateException("Attempting to send score without registering a color");
        }

        String stringScore = Integer.toString(score);
        output.println(NetworkMessage.generateScoresAndGameResults(stringScore, clientColor));
    }

    /**
     * An implementation of the NetworkObserver interface.
     * Handles receiving information from the server.
     */
    public class InputHandler implements NetworkObserver {
        // This queue contains all the DrawInfo objects received from the server
        private final ConcurrentLinkedQueue<DrawInfo> drawInfoQueue;


        private InputHandler() {
            drawInfoQueue = new ConcurrentLinkedQueue<>();
        }

        public boolean areInputsAvailable() {
            return !drawInfoQueue.isEmpty();
        }

        public DrawInfo getNextInput() {
            return drawInfoQueue.poll();
        }


        /**
         * This function is called everytime the client receives any message from the server.
         * Note: This function runs in the ClientNetworkThread
         * @param message The message received from the server
         */
        @Override
        public void messageReceived(String message) {
            String header = message.split("-", 2)[0];
            String data = message.split("-", 2)[1];

            switch(header) {
                case NetworkMessage.DRAW_MESSAGE_HEADER:
                    drawInfoQueue.add(DrawInfo.fromJson(data));
                    break;

                    // Both cases result in the same code
                case NetworkMessage.CANVAS_REQUEST_HEADER:
                case NetworkMessage.COLOR_REQUEST_HEADER:
                    // The main thread waits until there is something in the queue. This will wake up the main thread.
                    serverBoolResponseQueue.add(Boolean.parseBoolean(data));
                    break;
                case NetworkMessage.CALCULATE_SCORE_AND_GET_RESULTS:
                    String winnerScore = data.split("/")[0];
                    String stringColor = data.split("/")[1];

                    Color winnerColor = Color.valueOf(stringColor);
                    int score = Integer.parseInt(winnerScore);
                    GameResults results = new GameResults(score, winnerColor);
                    Game.GameEndResults endResults = new Game.GameEndResults(results);
                    Platform.runLater(() -> {
                        endResults.run();
                    });

                    break;
                case NetworkMessage.CANVAS_LOCK:
                    break;
                case NetworkMessage.CANVAS_CLEAR:
                    DrawInfo clear = new DrawInfo(0, 0, Integer.parseInt(data), Color.TRANSPARENT, false, true, false);
                    drawInfoQueue.add(clear);
                    break;
                case NetworkMessage.CANVAS_OWN:
                    String[] msg = data.split("/", 2);
                    Color color = Color.valueOf(msg[1]);
                    DrawInfo own = new DrawInfo(0, 0, Integer.parseInt(msg[0]), color, false, false, true);
                    drawInfoQueue.add(own);
                    break;
                default:
                    throw new IllegalArgumentException("Received invalid network message");
            }

        }
    }


}
