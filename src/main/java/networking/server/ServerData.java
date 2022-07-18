package networking.server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

class ServerData {

	// Clients are identified via the hash of the socket obtained through the socket.HashCode() method
	private static ServerData instance = null;
	public final LinkedBlockingQueue<ServerMessage> messageQueue;
	public final List<Socket> clientSockets;
	public final List<PrintWriter> clientOutputs;

	// A map to associate a given client with a given color
	// It maps the Socket hashcode to the color hashcode using the .HashCode method
	public final Map<Integer, Integer> clientColors;

	// A non-thread safe map containing all the canvases currently being used
	// and the hashcode of the socket of the user.
	// format is socket hashcode, canvasID
	public final Map<Integer, Integer> canvasesInUse;

	public static ServerData getInstance() {
		if(instance == null) {
			instance = new ServerData();
		}
		
		return instance;
	}
	
	private ServerData() {
		messageQueue = new LinkedBlockingQueue<>();
		clientOutputs = new ArrayList<>();
		clientSockets = new ArrayList<>();
		clientColors = new HashMap<>();
		canvasesInUse = new HashMap<>();
	}

	public int clientCount() {
		return clientOutputs.size();
	}

	public void removeClient(Socket clientSocket, PrintWriter clientOutput) {
		clientSockets.remove(clientSocket);
		clientOutputs.remove(clientOutput);

		clientColors.remove(clientSocket.hashCode());
		canvasesInUse.remove(clientSocket.hashCode());
	}
}

class ServerMessage {
	public final String message;
	public final int clientHashcode;
	public ServerMessage(String msg, int hashCode) {
		message = msg;
		clientHashcode = hashCode;
	}
}