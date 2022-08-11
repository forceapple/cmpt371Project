package networking;

import com.example.javafxtest.DrawInfo;
import com.example.javafxtest.LobbyPlayer;
import javafx.scene.paint.Color;

/**
 * A class containing static fields and methods related to sending messages over the network.
 *
 * All network messages consist of a header + data with the two being separated by a '-' character
 */
public class NetworkMessage {
    /**
     * Message Formats:
     *      Draw Messages:
     *          Use the DRAW_MESSAGE_HEADER + Gson serialization of a DrawInfo object
     *          The format is the same for messages to and from the server
     *
     *      Colour Request Messages:
     *          Sending Request to Server:
     *              Use the COLOR_REQUEST_HEADER + The string representation of the color has code.
     *              Obtained by Integer.toString(color.HashCode())
     *
     *          Server Responses:
     *              Use the same COLOR_REQUEST_HEADER + The .toString of a boolean obtained by Boolean.toString(value).
     *              True indicates that the request was allowed and the sender now owns the color
     *              False indicates that the colour was already in use by another client
     *
     *
     *      Canvas Request Messages:
     *          Sending Request to Server:
     *              Use the CANVAS_REQUEST_HEADER + The .toString of an int indicating the canvas id.
     *
     *          Server Responses:
     *              Use the CANVAS_REQUEST_HEADER + The .toString of a boolean obtained by Boolean.toString(value).
     *              True indicates the request was successful and the sender now owns the canvas
     *              False indicates the canvas was in use by another client
     *
     *      Canvas Release Messages:
     *          Use the CANVAS_RELEASE_HEADER and an empty data section
     *          The server does not send a response back for these messages
     *
     *
     *      Canvas Lock message:
     *          Sending Request to Server:
     *              Uses CANVAS_LOCK + .toString of an int indicating which canvas id to lock and sends it to the server
     *
     *
 *          Canvas Clear message:
     *           Sending Request to Server:
     *                 Uses CANVAS_CLEAR + .toString of an int indicating which canvas id to clear and
     *                 sends it to the server
     *
     *            Server Responses:
     *                 Uses CANVAS_CLEAR + .toString of an int indicating which canvas id to clear. Currently, it
     *                 relays which canvas to clear to all clients.
     *
     *      Calculate Score and Results message:
     *           Sending Request to Server:
     *                 Uses SCORE_AND_RESULTS + .toString of an int indicating score of player + color indicating client Color
     *                 and sends it to the server
     *
     *          Server Responses:
     *                 Uses SCORE_AND_RESULTS + int score indicating the score of the winner or tie score and the color indicating
     *                 which player won the game or transparent color if there is a tie +  and sends it to all
     *                 clients.
     *
     *      Lobby join message:
     *          Uses the LOBBY_PLAYER_JOIN_HEADER + The .toString of the joining player's color + '/' + A string representing the player's name
     *          The format of this message is the same when sent to or from the server. It is just forwarded to all connected clients.
     *
     *      Lobby left message:
     *          Uses the LOBBY_PLAYER_LEFT_HEADER + the .toString of the leaving player's color.
     *          This message is only ever sent from the server to all connected clients
     *
     *      Lobby player ready message:
     *          Uses the LOBBY_PLAYER_READY_HEADER + the .toString of the player's color + '/' + .toString of a boolean indicating the player's new ready status
     *          This message is the same when sent to or from the server
     *
     *      Lobby start countdown message:
     *          Uses the LOBBY_START_COUNTDOWN_HEADER and an empty data section.
     *          This message is only ever sent from the server to the clients. It indicates that all players are ready and the countdown timer should start
     *
     *
     *
     */
    public static final String DRAW_MESSAGE_HEADER = "DRAW";
    public static final String COLOR_REQUEST_HEADER = "COLOR_REQUEST";
    public static final String CANVAS_REQUEST_HEADER = "CANVAS_REQUEST";
    public static final String CANVAS_RELEASE_HEADER = "CANVAS_RELEASE";
    public static final String CANVAS_LOCK = "CANVAS_LOCK";
    public static final String CANVAS_CLEAR = "CANVAS_CLEAR";
    public static final String CANVAS_OWN = "CANVAS_OWN";
    public static final String CALCULATE_SCORE_AND_GET_RESULTS = "SCORE_AND_RESULTS";
    public static final String LOBBY_PLAYER_JOIN_HEADER = "LOBBY_PLAYER_JOIN";
    public static final String LOBBY_PLAYER_LEFT_HEADER = "LOBBY_PLAYER_LEFT";
    public static final String LOBBY_PLAYER_READY_HEADER = "LOBBY_PLAYER_READY";
    public static final String LOBBY_START_COUNTDOWN_HEADER = "LOBBY_START_COUNTDOWN";

    public static String addDrawMessageHeader(String msg) {
        return DRAW_MESSAGE_HEADER + "-" + msg;
    }

    public static String addColorRequestHeader(String msg) {
        return COLOR_REQUEST_HEADER + "-" + msg;
    }

    public static String addCanvasRequestHeader(String msg) {
        return CANVAS_REQUEST_HEADER + "-" + msg;
    }
    public static String addCanvasLockRequestHeader(String msg) {
        return CANVAS_LOCK + "-" + msg;
    }
    public static String addCanvasClearRequestHeader(String msg) {return CANVAS_CLEAR + "-" + msg; }
    public static String addCanvasOwnRequestHeader(String msg, Color ownedColor) {return CANVAS_OWN + "-" + msg + "/" + ownedColor; }

    public static String addLobbyPlayerJoinHeader(String msg) {
        return LOBBY_PLAYER_JOIN_HEADER + "-" + msg;
    }

    public static String addLobbyPlayerReadyHeader(String msg) {
        return LOBBY_PLAYER_READY_HEADER + "-" + msg;
    }

    public static String addLobbyPlayerLeftHeader(String msg) {
        return LOBBY_PLAYER_LEFT_HEADER + "-" + msg;
    }

    /**
     * Generates a message indicating that a player has joined the lobby
     * @param player The player who joined the lobby
     * @return The string encoding of the message
     */
    public static String generateLobbyPlayerJoinMessage(LobbyPlayer player) {
        return addLobbyPlayerJoinHeader(player.getPlayerColor().toString() + "/" + player.getPlayerName());
    }

    /**
     * Generates a message indicating the ready status of a player
     * @param player The player whose status is being changed
     * @param isReady The new ready status of the player
     * @return The string encoding of the message
     */
    public static String generateLobbyPlayerReadyMessage(LobbyPlayer player, boolean isReady) {
        return addLobbyPlayerReadyHeader(player.getPlayerColor().toString() + "/" + Boolean.toString(isReady));
    }

    /**
     * Generates a message indicating the player has left the lobby
     * @param player The player who has left
     * @return The string encoding of the message
     */
    public static String generateLobbyPlayerLeftMessage(LobbyPlayer player) {
        return addLobbyPlayerLeftHeader(player.getPlayerColor().toString());
    }

    /**
     * Generates a message indicating to the clients that the game start countdown should begin
     * @return The string encoding of the message
     */
    public static String generateLobbyStartCountdownMessage() {
        return LOBBY_START_COUNTDOWN_HEADER + "-";
    }

    /**
     * Generates a message indicating to the server the score of the client and client's color
     * @return The end results of the game
     */
    public static String generateScoresAndGameResults (String msg, Color clientColour){return CALCULATE_SCORE_AND_GET_RESULTS + "-" + msg + "/" + clientColour; }

    /**
     * Generates a message indicating to the server to release the currently owned canvas
     * @return The string encoding of the message
     */
    public static String generateCanvasReleaseMessage() {
        return CANVAS_RELEASE_HEADER + "-";
    }

    /**
     * Generates a draw message that can be sent through the server
     * @param drawing The DrawInfo object containing the data about the drawing
     * @return The message that should be sent through the server
     */
    public static String generateDrawMessage(DrawInfo drawing) {
        return addDrawMessageHeader(drawing.toJson());
    }


}
