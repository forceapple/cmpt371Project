package networking.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class NetworkClient {
    private ClientNetworkThread networkThread;
    private PrintWriter output;
    private BufferedReader input;
    private Socket socket;
    private List<NetworkObserver> observers;

    public NetworkClient() {
        observers = new ArrayList<>();

        try {
            socket = new Socket("127.0.0.1", 7070);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addObserver(NetworkObserver obs) {
        this.observers.add(obs);
    }

    public void removeObserver(NetworkObserver obs) {
        this.observers.remove(obs);
    }

    public void startClient() {
        networkThread = new ClientNetworkThread(input, observers);
        networkThread.setName("Client Network Thread");
        networkThread.setDaemon(true);
        networkThread.start();
    }

    public void sendMessage(String msg) {
        output.println(msg);
    }

}
