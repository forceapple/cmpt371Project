package networking;

import com.example.javafxtest.DrawInfo;
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
     *         Server Responses:
     *               Uses CANVAS_LOCK + .toString of an int indicating which canvas id to lock. Currently, it
     *               only relays the message to all other clients
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
     *
     */
    public static final String DRAW_MESSAGE_HEADER = "DRAW";
    public static final String COLOR_REQUEST_HEADER = "COLOR_REQUEST";
    public static final String CANVAS_REQUEST_HEADER = "CANVAS_REQUEST";
    public static final String CANVAS_RELEASE_HEADER = "CANVAS_RELEASE";
    public static final String CANVAS_LOCK = "CANVAS_LOCK";
    public static final String CANVAS_CLEAR = "CANVAS_CLEAR";
    public static final String CANVAS_OWN = "CANVAS_OWN";
    public static final String CANVAS_LOCK_CHECK = "CANVAS_LOCK_CHECK";

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
    public static String addCanvasIsLockedRequestHeader(String msg) {
        return CANVAS_LOCK_CHECK + "-" + msg;
    }

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
