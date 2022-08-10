package networking.server;

import javafx.scene.paint.Color;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

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

	// A map to associate a given client with their score
	public final Map<Color, Integer> clientScores;
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
		clientScores = new HashMap<>();

		isLocked = new Boolean[8];

		for(int i=0; i<8; i++){
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
		synchronized(isLocked) {
			this.isLocked[canvasID] = true;
		}
	}

	/*
	once a client earn a score, this method is called and in this method hashmap clientScores have a key as
	client colour and value as client score and once all canvases are locked i.e. they are coloured then a boolean
	is returned which determines the end of the game
	 */
	public boolean storeScore(Color color, int score){

         clientScores.put(color, score);

		//checking if all canvases are coloured
		boolean allTrue = true;
		for (boolean i : isLocked) {
			if (!i) {
				allTrue = false;
				break;
			}
		}
		return allTrue;
	}

	//checking if a player won a game or if there is a tie
	public Map<Color, Integer> checkResult(){
		int highestScore = Collections.max(clientScores.values());// This will return highest Score

		Map <Color, Integer> winners = new HashMap<>();

		for (Map.Entry<Color, Integer> entry : clientScores.entrySet()) {  // Iterate through hashmap
			if (entry.getValue() == highestScore) {
				winners.put(entry.getKey(), entry.getValue());
			}
		}
		return winners;
	}



}
