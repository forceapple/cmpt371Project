package networking.server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

class ServerData {
	
	private static ServerData instance = null;
	
	public LinkedBlockingQueue<ServerMessage> messageQueue;
	public List<Socket> clientSockets;
	public List<PrintWriter> clientOutputs;
	
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
	}

	public int clientCount() {
		return clientOutputs.size();
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