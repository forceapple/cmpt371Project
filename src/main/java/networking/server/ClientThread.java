package networking.server;

import networking.NetworkMessage;

import java.io.*;
import java.net.*;

public class ClientThread extends Thread {
	private Socket socket;
	private ServerData server;
	private PrintWriter output;
	
	public ClientThread(Socket socket) {
		this.socket = socket;
		server = ServerData.getInstance();
	}
	
	public void run() {
		System.out.println("Client Thread Starting");
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);
			server.clientOutputs.add(output);
			server.clientSockets.add(socket);
			
			while(true) {
				processMessage(input.readLine());
			}
		}
		catch(IOException ex) {
			System.out.println("Exception on Server: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	private void processMessage(String msg) {

		String header = msg.split("-", 2)[0];


		String data = msg.split("-", 2)[1];

		if(header.equals(NetworkMessage.DRAW_MESSAGE_HEADER)) {
			processDrawMessage(data);
		}

		else if(header.equals(NetworkMessage.CANVAS_REQUEST_HEADER)) {
			processCanvasRequest(data);
		}

		else if(header.equals(NetworkMessage.CANVAS_RELEASE_HEADER)) {
			processCanvasRelease();
		}

		else if (header.equals(NetworkMessage.COLOR_REQUEST_HEADER)) {
			processColorRequest(data);
		}

		else {
			// TODO: Don't throw exception on server, instead sent some error to client
			throw new IllegalArgumentException("Invalid Message Being Sent over network");
		}



	}

	private void processDrawMessage(String data) {
		try {
			server.messageQueue.put(new ServerMessage(data, socket.hashCode()));
		}
		catch(InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	private void processCanvasRequest(String data) {
		int canvasID = Integer.parseInt(data);
		synchronized(server.canvasesInUse) {
			if(server.canvasesInUse.containsValue(canvasID) && server.canvasesInUse.get(socket.hashCode()) != canvasID) {
				output.println(NetworkMessage.addCanvasRequestHeader(Boolean.toString(false)));
			}
			else {
				server.canvasesInUse.put(socket.hashCode(), canvasID);
				output.println(NetworkMessage.addCanvasRequestHeader(Boolean.toString(true)));
			}
		}
	}

	private void processCanvasRelease() {
		server.canvasesInUse.remove(socket.hashCode());
	}

	private void processColorRequest(String data) {
		int colorHash = Integer.parseInt(data);
		synchronized(server.clientColors) {
			if(server.clientColors.containsValue(colorHash)) {
				output.println(NetworkMessage.addColorRequestHeader(Boolean.toString(false)));
			}
			else {
				server.clientColors.put(socket.hashCode(), colorHash);
				output.println(NetworkMessage.addColorRequestHeader(Boolean.toString(true)));
			}
		}
	}
}
