package networking.server;

import com.example.javafxtest.DrawInfo;
import networking.NetworkMessage;

public class ServerOutputThread extends Thread{
	
	private ServerData server;
	
	public ServerOutputThread() {
		server = ServerData.getInstance();
	}
	
	public void run() {
		System.out.println("Server Output Thread Starting");
		ServerMessage msg = new ServerMessage("", "", -1);
		while(true) {
			try {
				msg = server.messageQueue.take();
				processDrawMessage(msg);

				System.out.println("Header: " + msg.header);

				if(msg.header.equals(NetworkMessage.DRAW_MESSAGE_HEADER)) {
					//processDrawMessage(msg);
				}

			}
			catch(InterruptedException ex) {
				ex.printStackTrace();
			}

		}
	}

	private void processDrawMessage(ServerMessage message) {
		System.out.println("processDrawMessage: " + message.generateFullMessage());
		for(int i = 0; i < server.clientCount(); i++) {

			// Send the message to everyone except the one who sent it
			if(message.clientHashcode != server.clientSockets.get(i).hashCode()) {
				server.clientOutputs.get(i).println(message.generateFullMessage());
			}

		}
	}

	private void processCanvasRequestMessage(ServerMessage message) {

	}
	

}
