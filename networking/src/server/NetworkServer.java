package server;

import java.net.*;
import java.io.*;

public class NetworkServer extends Thread{
	private int port;
	
	public NetworkServer(int port) {
		this.port = port;	
	}
	
	public void run() {
		System.out.println("Starting Server");
		
		
		ServerOutputThread outputThread = new ServerOutputThread();
		outputThread.setDaemon(true);
		outputThread.setName("Server Output Thread");
		outputThread.start();

		try {
			ServerSocket serverSocket = new ServerSocket(port);
			System.out.println("Server is listening on port: " + port);
			
			while(true) {
				Socket socket = serverSocket.accept();
				System.out.println("New connection");
				
				ClientThread clientThread = new ClientThread(socket);
				clientThread.setDaemon(true);
				clientThread.setName("Client Thread: " + socket.getInetAddress().toString());
				clientThread.start();
			}
			
		}
		catch(IOException ex) {
			System.out.println("Exception on Server: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
}
