package com.example.javafxtest;

import com.google.gson.Gson;

public class DrawInfo {
    public double x;
    public double y;
    public int canvasId;


    public DrawInfo(int id, double x, double y) {
        this.canvasId = id;
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getCanvasId() {
        return canvasId;
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
