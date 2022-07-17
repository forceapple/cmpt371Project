package networking.server;

import java.io.*;
import java.net.Socket;

public class ServerOutputThread extends Thread{
	
	private ServerData server;
	
	public ServerOutputThread() {
		server = ServerData.getInstance();
	}
	
	public void run() {
		System.out.println("Server Output Thread Starting");
		ServerMessage msg;
		while(true) {
			try {
				msg = server.messageQueue.take();
				
				for(int i = 0; i < server.clientCount(); i++) {

					// Send the message to everyone except the one who sent it
					if(msg.clientHashcode != server.clientSockets.get(i).hashCode()) {
						server.clientOutputs.get(i).println(msg.message);
					}

				}
			}
			catch(InterruptedException ex) {
				ex.printStackTrace();
			}

		}
	}
	
	

}
