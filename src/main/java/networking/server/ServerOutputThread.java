package networking.server;

import com.example.javafxtest.DrawInfo;
import networking.NetworkMessage;

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

				DrawInfo info = DrawInfo.fromJson(msg.message);

				int colorHash = info.getColor().hashCode();
				int canvasID = info.getCanvasId();

				if(server.clientColors.get(msg.clientHashcode) != colorHash) {
					// TODO: Implement sending errors to the client
					throw new IllegalStateException("Attempting to draw with an unregistered colour!");
				}

				if(server.canvasesInUse.get(msg.clientHashcode) != canvasID) {
					throw new IllegalStateException("Attempting to draw on an canvas that isn't registered to the user");
				}

				for(int i = 0; i < server.clientCount(); i++) {

					// Send the message to everyone except the one who sent it
					if(msg.clientHashcode != server.clientSockets.get(i).hashCode()) {
						server.clientOutputs.get(i).println(NetworkMessage.addDrawMessageHeader(msg.message));
					}

				}
			}
			catch(InterruptedException ex) {
				ex.printStackTrace();
			}

		}
	}
	
	

}
