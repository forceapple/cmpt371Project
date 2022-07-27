package networking.server;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A singleton class that contains all the data that needs to be shared across the different server threads
 * Accessing variables is not thread safe so manual synchronization is required.
 */
class ServerData {

	// Clients are identified via the hash of the socket obtained through the socket.HashCode() method
	private static ServerData instance = null;

	/**
	 * All accesses to variables belonging to this class MUST be encased in a synchronized block
	 * to prevent multiple threads from accessing them at the same time
	 */

	public final List<Socket> clientSockets;
	public final List<PrintWriter> clientOutputs;

	// A map to associate a given client with a given color
	// It maps the Socket hashcode to the color hashcode using the .HashCode method
	public final Map<Integer, Integer> clientColors;

	// A non-thread safe map containing all the canvases currently being used
	// and the hashcode of the socket of the user.
	// format is socket hashcode, canvasID
	public final Map<Integer, Integer> canvasesInUse;
	public Boolean[] isLocked;

	public static ServerData getInstance() {
		if(instance == null) {
			instance = new ServerData();
		}
		
		return instance;
	}
	
	private ServerData() {
		clientOutputs = new ArrayList<>();
		clientSockets = new ArrayList<>();
		clientColors = new HashMap<>();
		canvasesInUse = new HashMap<>();
		isLocked = new Boolean[64];

		for(int i=0; i<64; i++){
			isLocked[i] = false;
		}

	}

	public int clientCount() {
		synchronized(clientOutputs) {
			return clientOutputs.size();
		}
	}

	public void removeClient(Socket clientSocket, PrintWriter clientOutput) {
		synchronized(clientSockets) {
			clientSockets.remove(clientSocket);
		}
		synchronized(clientOutputs) {
			clientOutputs.remove(clientOutput);
		}
		synchronized(clientColors) {
			clientColors.remove(clientSocket.hashCode());
		}
		synchronized(canvasesInUse) {
			canvasesInUse.remove(clientSocket.hashCode());
		}
	}

	public void lockCanvasByID(int canvasID) {
		this.isLocked[canvasID] = true;
		System.out.println(isLocked[canvasID]);
	}
}
