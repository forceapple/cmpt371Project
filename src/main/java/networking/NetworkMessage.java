package networking;

import com.example.javafxtest.DrawInfo;

public class NetworkMessage {
    public static final String DRAW_MESSAGE_HEADER = "DRAW";
    public static final String COLOR_REQUEST_HEADER = "COLOR_REQUEST";
    public static final String CANVAS_REQUEST_HEADER = "CANVAS_REQUEST";
    public static final String CANVAS_RELEASE_HEADER = "CANVAS_RELEASE";

    public static String addDrawMessageHeader(String msg) {
        return DRAW_MESSAGE_HEADER + "-" + msg;
    }

    public static String addColorRequestHeader(String msg) {
        return COLOR_REQUEST_HEADER + "-" + msg;
    }

    public static String addCanvasRequestHeader(String msg) {
        return CANVAS_REQUEST_HEADER + "-" + msg;
    }

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
