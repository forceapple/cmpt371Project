package networking.server;

import com.example.javafxtest.DrawInfo;
import networking.NetworkMessage;

import java.io.*;
import java.net.*;

@SuppressWarnings("SynchronizeOnNonFinalField") // To disable warning about synchronizing on output
/**
 * ClientThread is the thread that is created for every new client that connects to the server.
 * It handles all the messages sent to the server from the client and is also responsible for sending responses
 */
public class ClientThread extends Thread {
	private final Socket socket;
	private final ServerData server;

	// Using output in any way requires synchronization since the output can be accessed by other threads at any time
	private PrintWriter output;
	private BufferedReader input;
	public ClientThread(Socket socket) {
		this.socket = socket;
		server = ServerData.getInstance();
	}
	
	public void run() {
		System.out.println("Client Thread Starting");
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);


			synchronized(server.clientOutputs) {
				server.clientOutputs.add(output);
			}
			synchronized (server.clientSockets) {
				server.clientSockets.add(socket);
			}

			while(true) {
				// input.readLine is a blocking method so this thread will wait here until it receives an input
				processMessage(input.readLine());
			}
		}
		// SocketException should mean that the client disconnected
		catch(SocketException ex) {
			server.removeClient(socket, output);
			System.out.println("Client Disconnected (" + socket.getInetAddress().toString()
					+ ":" + socket.getPort() + ")");

			// I'm not completely sure if these need to be closed, but it shouldn't hurt to explicitly close them
			try {
				socket.close();
				output.close();
				input.close();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		catch(IOException ex) {
			System.out.println("Exception on Server: " + ex.getMessage());
			ex.printStackTrace();
		}

		System.out.println("Client Thread Stopping");
	}

	private void processMessage(String msg) {
		// Messages are split into 2 parts.
		// The header indicates what the message is and data contains the contents of the message
		// The header and data are separated by a '-' character. See NetworkMessage.java for details

		String header = msg.split("-", 2)[0];
		String data = msg.split("-", 2)[1];

		// Call the appropriate function based on the header
		switch (header) {
			case NetworkMessage.DRAW_MESSAGE_HEADER:
				processDrawMessage(data);
				break;
			case NetworkMessage.CANVAS_REQUEST_HEADER:
				processCanvasRequest(data);
				break;
			case NetworkMessage.CANVAS_RELEASE_HEADER:
				processCanvasRelease();
				break;
			case NetworkMessage.COLOR_REQUEST_HEADER:
				processColorRequest(data);
				break;
			case NetworkMessage.CANVAS_LOCK:
				System.out.println("ClientThread");
				processLockMessage(data);
				break;
			case NetworkMessage.CANVAS_CLEAR:
				processCanvasClearMessage(data);
				break;
			default:
				// TODO: Don't throw exception on server, instead sent some error to client
				throw new IllegalArgumentException("Invalid Message being sent over network");
		}
	}

	private void processDrawMessage(String data) {
		DrawInfo info = DrawInfo.fromJson(data);

		int colorHash = info.getColor().hashCode();
		int canvasID = info.getCanvasID();

		// Make sure that the colour and canvas are valid
		synchronized (server.clientColors) {
			if(server.clientColors.get(socket.hashCode()) == null || server.clientColors.get(socket.hashCode()) != colorHash) {
				// TODO: Implement sending errors to the client
				throw new IllegalStateException("Attempting to draw with an unregistered colour!");
			}
		}
		synchronized(server.canvasesInUse) {
			if(server.canvasesInUse.get(socket.hashCode()) == null || server.canvasesInUse.get(socket.hashCode()) != canvasID) {
				throw new IllegalStateException("Attempting to draw on an canvas that isn't registered to the user");
			}
		}

		// Send the draw message to all connected clients except for the one who sent it
		// A lock MUST be acquired on the client outputs to ensure that clients are not added or deleted while iterating
		synchronized (server.clientOutputs) {
			for(PrintWriter out : server.clientOutputs) {
				// A lock MUST be acquired on the PrintWriters to ensure that two threads do not send messages at the exact same time
				// Disable the warning. Although the compiler thinks out is a local variable it actually isn't
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized(out) {
					if(!out.equals(output)) {
						out.println(NetworkMessage.addDrawMessageHeader(data));
					}
				}
			}
		}
	}

	private void processCanvasRequest(String data) {
		int canvasID = Integer.parseInt(data);

		synchronized(server.canvasesInUse) {
			// Send true or false depending on if the canvas is already owned
			if(server.canvasesInUse.containsValue(canvasID) ) {
				synchronized(output) {
					output.println(NetworkMessage.addCanvasRequestHeader(Boolean.toString(false)));
				}
			}
			else {
				server.canvasesInUse.put(socket.hashCode(), canvasID);
				synchronized(output) {
					output.println(NetworkMessage.addCanvasRequestHeader(Boolean.toString(true)));
				}
			}
		}
	}

	private void processCanvasRelease() {
		synchronized(server.canvasesInUse) {
			server.canvasesInUse.remove(socket.hashCode());
		}
	}

	private void processColorRequest(String data) {
		int colorHash = Integer.parseInt(data);
		synchronized(server.clientColors) {
			if(server.clientColors.containsValue(colorHash)) {
				synchronized(output) {
					output.println(NetworkMessage.addColorRequestHeader(Boolean.toString(false)));
				}
			}
			else {
				server.clientColors.put(socket.hashCode(), colorHash);
				synchronized(output) {
					output.println(NetworkMessage.addColorRequestHeader(Boolean.toString(true)));
				}
			}
		}
	}

	private void processLockMessage(String data) {
		synchronized (server.islockedMutex){
			for(PrintWriter out : server.clientOutputs) {
				out.println(NetworkMessage.addCanvasLockRequestHeader(data));
			}
		}
	}

	private void processCanvasClearMessage(String data) {
		synchronized (server.clientOutputs) {
			for (PrintWriter out : server.clientOutputs) {
				out.println(NetworkMessage.addCanvasClearRequestHeader(data));
			}
		}
	}
}
