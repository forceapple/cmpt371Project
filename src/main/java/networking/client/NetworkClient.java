package networking.client;

import com.example.javafxtest.DrawInfo;
import javafx.scene.paint.Color;
import networking.NetworkMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkClient {
    private ClientNetworkThread networkThread;
    private PrintWriter output;
    private BufferedReader input;
    private Socket socket;
    private List<NetworkObserver> observers;

    public Color clientColor = null;
    public final InputHandler networkInputs;

    public int currentCanvasID;

    // serverResponseBoolSync is only used as a synchronization lock for serverResponseBool
    // This thread will call await() on serverResponseSync, and it will be woken up
    // when there is a server response which is saved in serverResponseBool
    private final Boolean serverResponseBoolSync;
    private boolean serverResponseBool;


    public NetworkClient() {
        observers = new ArrayList<>();
        networkInputs = new InputHandler();
        addObserver(networkInputs);
        serverResponseBoolSync = false;
        serverResponseBool = false;
        currentCanvasID = -1;

        try {
            socket = new Socket("127.0.0.1", 7070);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        networkThread = new ClientNetworkThread(input, observers);
        networkThread.setName("Client Network Thread");
        networkThread.setDaemon(true);
        networkThread.start();
    }

    public void addObserver(NetworkObserver obs) {
        this.observers.add(obs);
    }

    public void removeObserver(NetworkObserver obs) {
        this.observers.remove(obs);
    }

    public void startClient() {

        if(clientColor == null) {
            throw new IllegalStateException("Attempting to start the client without registering a colour!");
        }

    }

    /**
     * Attempts to register a colour with the server
     * @param color The colour being registered
     * @return Returns true if the colour is successfully registered and false if the colour is already in use
     */
    public boolean registerColor(Color color) {
        output.println(NetworkMessage.addColorRequestHeader(Integer.toString(color.hashCode())));

        synchronized(serverResponseBoolSync) {
            try {
                serverResponseBoolSync.wait();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if(serverResponseBool) {
            clientColor = color;
        }

        return serverResponseBool;
    }

    /**
     * Attempts to select a canvas for drawing. If successful then the canvas will be acquired by client
     * and be unable to be used by any other client until the releaseCanvas function is called
     * @param canvasId The ID of the canvas
     * @return true on success, false on failure
     */
    public boolean selectCanvasForDrawing(int canvasId) {

        output.println(NetworkMessage.addCanvasRequestHeader(Integer.toString(canvasId)));

        synchronized(serverResponseBoolSync) {
            try {
                serverResponseBoolSync.wait();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if(serverResponseBool) {
            currentCanvasID = canvasId;
        }

        return serverResponseBool;
    }

    /**
     * Releases the canvas owned by the client. If no canvases are owned then does nothing
     */
    public void releaseCanvas() {
        output.println(NetworkMessage.generateCanvasReleaseMessage());
        currentCanvasID = -1;
    }

    public void sendDrawingMessage(String msg) {
        output.println(NetworkMessage.addDrawMessageHeader(msg));
    }

    public class InputHandler implements NetworkObserver {
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

        @Override
        public void messageReceived(String message) {
            String header = message.split("-", 2)[0];
            String data = message.split("-", 2)[1];

            if(header.equals(NetworkMessage.DRAW_MESSAGE_HEADER)) {
                drawInfoQueue.add(DrawInfo.fromJson(data));
            }

            else if(header.equals(NetworkMessage.CANVAS_REQUEST_HEADER)) {
                synchronized(serverResponseBoolSync) {
                    serverResponseBool = Boolean.parseBoolean(data);
                    serverResponseBoolSync.notify();
                }
            }

            else if (header.equals(NetworkMessage.COLOR_REQUEST_HEADER)) {
                synchronized(serverResponseBoolSync) {
                    serverResponseBool = Boolean.parseBoolean(data);
                    serverResponseBoolSync.notify();
                }
            }

            else {
                throw new IllegalArgumentException("Received invalid network message");
            }

        }
    }

}
