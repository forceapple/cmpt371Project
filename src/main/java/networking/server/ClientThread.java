package networking.server;

import java.io.*;
import java.net.*;

public class ClientThread extends Thread {
	private Socket socket;
	private ServerData server;
	
	
	public ClientThread(Socket socket) {
		this.socket = socket;
		server = ServerData.getInstance();
	}
	
	public void run() {
		System.out.println("Client Thread Starting");
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
			server.clientOutputs.add(output);
			server.clientSockets.add(socket);

			String msg;
			
			while(true) {
				msg = input.readLine();
				
				server.messageQueue.put(new ServerMessage(msg, socket.hashCode()));
			}
		}
		catch(IOException ex) {
			System.out.println("Exception on Server: " + ex.getMessage());
			ex.printStackTrace();
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
}
