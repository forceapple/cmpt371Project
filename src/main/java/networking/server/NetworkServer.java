package networking.server;

import java.net.*;
import java.io.*;

public class NetworkServer extends Thread{
	private final int port;
	
	public NetworkServer(int port) {
		this.port = port;	
	}
	
	public void run() {
		System.out.println("Starting Server");


		try(ServerSocket serverSocket = new ServerSocket(port)){
			ServerData.getInstance().setServerSocket(serverSocket);
			System.out.println("Server is listening on port: " + port);
			
			while(true) {
				Socket socket = serverSocket.accept();
				System.out.println("New connection from: " + socket.getInetAddress().toString() + ":" + socket.getPort());
				
				ClientThread clientThread = new ClientThread(socket);
				clientThread.setDaemon(true);
				clientThread.setName("Client Thread: " + socket.getInetAddress().toString() + ":" + socket.getPort());
				clientThread.start();
			}
			
		}
		// This will occur if the server socket is closed. This happens when the game starts
		catch(SocketException ex) {
			System.out.println("Closing Server to new clients");
		}
		catch(IOException ex) {
			System.out.println("Exception on Server: " + ex.getMessage());
			ex.printStackTrace();
		}

	}
	
}
