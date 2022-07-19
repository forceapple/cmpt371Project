package networking.server;

import com.example.javafxtest.DrawInfo;
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
		// SocketException should mean that the client disconnected
		catch(SocketException ex) {
			server.removeClient(socket, output);

			// Not sure if the output is closed at this point, but it shouldn't hurt to explicitly close it
			output.close();
		}
		catch(IOException ex) {
			System.out.println("Exception on Server: " + ex.getMessage());
			ex.printStackTrace();
		}

		System.out.println("Client Thread Stopping");
	}

	private void processMessage(String msg) {

		String header = msg.split("-", 2)[0];
		String data = msg.split("-", 2)[1];

		if(header.equals(NetworkMessage.DRAW_MESSAGE_HEADER)) {
			processDrawMessage(header, data);
		}

		else if(header.equals(NetworkMessage.CANVAS_REQUEST_HEADER)) {
			processCanvasRequest(header, data);
		}

		else if(header.equals(NetworkMessage.CANVAS_RELEASE_HEADER)) {
			processCanvasRelease();
		}

		else if (header.equals(NetworkMessage.COLOR_REQUEST_HEADER)) {
			processColorRequest(header, data);
		}

		else {
			// TODO: Don't throw exception on server, instead sent some error to client
			throw new IllegalArgumentException("Invalid Message Being Sent over network");
		}
	}

	private void processDrawMessage(String header, String data) {
		DrawInfo info = DrawInfo.fromJson(data);

		int colorHash = info.getColor().hashCode();
		int canvasID = info.getCanvasID();

		if(server.clientColors.get(socket.hashCode()) != colorHash) {
			// TODO: Implement sending errors to the client
			throw new IllegalStateException("Attempting to draw with an unregistered colour!");
		}

		if(server.canvasesInUse.get(socket.hashCode()) != canvasID) {
			throw new IllegalStateException("Attempting to draw on an canvas that isn't registered to the user");
		}

		try {
			server.messageQueue.put(new ServerMessage(header, data, socket.hashCode()));
		}
		catch(InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	private void processCanvasRequest(String header, String data) {
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

	private void processColorRequest(String header, String data) {
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
