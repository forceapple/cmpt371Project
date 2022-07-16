package com.example.javafxtest;

import com.google.gson.Gson;
import javafx.scene.paint.Color;

public class DrawInfo {
    public double x;
    public double y;

    public DrawInfo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }


    public static String toJson(DrawInfo drawInfo) {
        Gson gson = new Gson();
        return gson.toJson(drawInfo);
    }

    public static DrawInfo fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, DrawInfo.class);
    }
}
