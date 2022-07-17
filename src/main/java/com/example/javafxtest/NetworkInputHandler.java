package com.example.javafxtest;

import networking.client.NetworkObserver;

import java.util.concurrent.LinkedBlockingQueue;

public class NetworkInputHandler implements NetworkObserver {

    public LinkedBlockingQueue<DrawInfo> drawInfoQueue;

    public NetworkInputHandler() {
        drawInfoQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void messageReceived(String message) {
        drawInfoQueue.add(DrawInfo.fromJson(message));
    }
}
