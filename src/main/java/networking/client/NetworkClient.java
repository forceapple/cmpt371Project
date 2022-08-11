package networking.client;

import com.example.javafxtest.*;
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

    // A blocking queue used for thread safe communication between the main application thread and the network thread
    // when registering a color or selecting a canvas
    private final BlockingQueue<Boolean> serverBoolResponseQueue;
    private boolean clientRunning = false;
    private boolean firstDraw = false;

    private LobbyControllerCallback lobbyCallback;


    public NetworkClient(String host, String port) throws IOException, IllegalArgumentException {
        serverBoolResponseQueue = new LinkedBlockingQueue<>();
        observers = new ArrayList<>();
        networkInputs = new InputHandler();
        addObserver(networkInputs);
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
     * Starts the lobby with the given lobby callback
     * @param lobbyCallback The callback class for the lobby
     * @param player The player who joined
     */
    public void startLobby(LobbyControllerCallback lobbyCallback, LobbyPlayer player) {
        this.lobbyCallback = lobbyCallback;
        output.println(NetworkMessage.generateLobbyPlayerJoinMessage(player));
    }

    public void setPlayerReady(LobbyPlayer player, boolean isReady) {
        output.println(NetworkMessage.generateLobbyPlayerReadyMessage(player, isReady));
    }

    /**
     * An implementation of the NetworkObserver interface.
     * Handles receiving information from the server.
     */
    public class InputHandler implements NetworkObserver {
        // This queue contains all the DrawInfo objects received from the server
        private final ConcurrentLinkedQueue<DrawInfo> drawInfoQueue;
        private final List<LobbyPlayer> lobbyPlayersList = new ArrayList<>();

        /**
         * Gets the index of a lobby player in the lobbyPlayersList by color.
         * @param color The color of the lobby player
         * @return The index of the lobby player. -1 if not found.
         */
        private int getLobbyPlayerIndexByColor(Color color) {
            for(int i = 0; i < lobbyPlayersList.size(); i++) {
                if(lobbyPlayersList.get(i).getPlayerColor().equals(color)) {
                    return i;
                }
            }

            return -1;
        }

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

                case NetworkMessage.LOBBY_PLAYER_JOIN_HEADER:
                    if(Objects.isNull(lobbyCallback)) {
                        break;
                    }

                    String[] fields = data.split("/", 2);
                    LobbyPlayer newPlayer = new LobbyPlayer(Color.valueOf(fields[0]), fields[1]);
                    if(newPlayer.getPlayerColor().equals(clientColor)) {
                        newPlayer = new LobbyPlayer(clientColor, "(You) " + fields[1]);
                        newPlayer.setPlayerIsUser(true);
                    }
                    lobbyPlayersList.add(newPlayer);
                    lobbyCallback.addPlayer(newPlayer);
                    break;
                case NetworkMessage.LOBBY_PLAYER_LEFT_HEADER:
                    if(Objects.isNull(lobbyCallback)) {
                        break;
                    }

                    int leftPlayerIndex = getLobbyPlayerIndexByColor(Color.valueOf(data));
                    lobbyCallback.removePlayer(leftPlayerIndex);
                    lobbyPlayersList.remove(leftPlayerIndex);
                    break;
                case NetworkMessage.LOBBY_PLAYER_READY_HEADER:
                    if(Objects.isNull(lobbyCallback)) {
                        break;
                    }

                    int readyPlayerIndex = getLobbyPlayerIndexByColor(Color.valueOf(data.split("/")[0]));
                    boolean isReady = Boolean.parseBoolean(data.split("/")[1]);
                    lobbyPlayersList.get(readyPlayerIndex).setPlayerReady(isReady);
                    lobbyCallback.setReady(readyPlayerIndex, isReady);
                    break;
                case NetworkMessage.LOBBY_START_COUNTDOWN_HEADER:
                    lobbyCallback.startGameCountdown();
                    break;
                default:
                    throw new IllegalArgumentException("Received invalid network message");
            }

        }
    }


}
