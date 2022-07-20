package com.example.javafxtest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import javafx.scene.paint.Color;

import java.io.IOException;

/**
 * An immutable class used to contain information about a user's drawing.
 * Used for sending information over the network.
 */
public class DrawInfo {
    private final double x;
    private final double y;
    private final int canvasID;
    private final Color color;

    public DrawInfo(double x, double y, int canvasID, Color color) {
        this.x = x;
        this.y = y;
        this.canvasID = canvasID;
        this.color = color;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getCanvasID() {
        return canvasID;
    }

    public Color getColor() {
        return color;
    }
    public String toJson() {
        return toJson(this);
    }
    public static String toJson(DrawInfo drawInfo) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DrawInfo.class, new DrawInfoAdapter());
        Gson gson = builder.create();
        return gson.toJson(drawInfo);
    }

    public static DrawInfo fromJson(String json) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DrawInfo.class, new DrawInfoAdapter());
        Gson gson = builder.create();
        return gson.fromJson(json, DrawInfo.class);
    }

}

/**
 * A TypeAdapter for the DrawInfo class
 * Used for Gson serialization
 */
class DrawInfoAdapter extends TypeAdapter<DrawInfo> {

    @Override
    public void write(JsonWriter jsonWriter, DrawInfo drawInfo) throws IOException {
        jsonWriter.beginObject();

        jsonWriter.name("x");
        jsonWriter.value(drawInfo.getX());
        jsonWriter.name("y");
        jsonWriter.value(drawInfo.getY());
        jsonWriter.name("canvasID");
        jsonWriter.value(drawInfo.getCanvasID());

        Color color = drawInfo.getColor();
        jsonWriter.name("color.Red");
        jsonWriter.value(color.getRed());
        jsonWriter.name("color.Green");
        jsonWriter.value(color.getGreen());
        jsonWriter.name("color.Blue");
        jsonWriter.value(color.getBlue());
        jsonWriter.name("color.Opacity");
        jsonWriter.value(color.getOpacity());

        jsonWriter.endObject();
    }

    @Override
    public DrawInfo read(JsonReader jsonReader) throws IOException {
        double x = 0;
        double y = 0;
        int canvasID = -1;
        Color color;
        double colorRed = 0;
        double colorGreen = 0;
        double colorBlue = 0;
        double colorOpacity = 0;

        jsonReader.beginObject();

        String propertyName = "";
        while(jsonReader.hasNext()) {
            JsonToken token = jsonReader.peek();

            if (token.equals(JsonToken.NAME)) {
                propertyName = jsonReader.nextName();
            }

            if (propertyName.equals("x")) {
                x = jsonReader.nextDouble();
            }
            if (propertyName.equals("y")) {
                y = jsonReader.nextDouble();
            }
            if (propertyName.equals("canvasID")) {
                canvasID = jsonReader.nextInt();
            }
            if (propertyName.equals("color.Red")) {
                colorRed = jsonReader.nextDouble();
            }
            if (propertyName.equals("color.Green")) {
                colorGreen = jsonReader.nextDouble();
            }
            if (propertyName.equals("color.Blue")) {
                colorBlue = jsonReader.nextDouble();
            }
            if (propertyName.equals("color.Opacity")) {
                colorOpacity = jsonReader.nextDouble();
            }
        }

        jsonReader.endObject();

        color = new Color(colorRed, colorGreen, colorBlue, colorOpacity);
        return new DrawInfo(x, y, canvasID, color);
    }
}










