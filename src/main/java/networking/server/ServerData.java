package networking.server;

import javafx.scene.paint.Color;
import networking.NetworkMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A singleton class that contains all the data that needs to be shared across the different server threads
 * Accessing variables is not thread safe so manual synchronization is required.
 */
class ServerData {

	/**
	 * Note: ClientID are the hashcode of the client's socket. Hashcodes are obtained by socket.HashCode()
	 */

	// The singleton instance of the class
	private static ServerData instance = null;

	/**
	 * A map to associate a given client with their PrintWriter output.
	 * The format is: ClientID, Output.
	 * An entry in this map will exist for all connected clients so this can be used to get the IDs of connected clients.
	 */
	private final Map<Integer, PrintWriter> clientOutputs;

	// A map to associate a given client with a given color
	// It maps the Socket hashcode to the color hashcode using the .HashCode method
	private final Map<Integer, Integer> clientColors;

	// A non-thread safe map containing all the canvases currently being used
	// and the hashcode of the socket of the user.
	// format is socket hashcode, canvasID
	private final Map<Integer, Integer> canvasesInUse;

	// A map to associate a given client with their score
	private final Map<Color, Integer> clientScores;
	private final Boolean[] isLocked;

	// A list that contains the clientIDs of all players in the lobby
	private final List<Integer> playersInLobby;
	// A list that contains the clientIDs of all players in the lobby who are ready
	private final List<Integer> readyPlayersInLobby;
	// A list that contains all the lobby messages sent
	private final List<String> lobbyMessagesList;

	private ServerSocket serverSocket;

	public static ServerData getInstance() {
		if(instance == null) {
			instance = new ServerData();
		}
		
		return instance;
	}
	
	private ServerData() {
		clientOutputs = new HashMap<>();
		clientColors = new HashMap<>();
		canvasesInUse = new HashMap<>();
		clientScores = new HashMap<>();
		playersInLobby = new ArrayList<>();
		readyPlayersInLobby = new ArrayList<>();
		lobbyMessagesList =  new LinkedList<>();

		isLocked = new Boolean[64];
		Arrays.fill(isLocked, false);
	}

	/**
	 * Adds a client to the server data.
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client
	 * @param clientOutput The client's PrintWriter output
	 */
	public synchronized void addClient(int clientID, PrintWriter clientOutput) {
		clientOutputs.put(clientID, clientOutput);
	}


	/**
	 * Removes a client.
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client being removed
	 */
	public synchronized void removeClient(int clientID) {
		clientColors.remove(clientID);
		canvasesInUse.remove(clientID);

		// The PrintWriter is closed in the ClientThread
		clientOutputs.remove(clientID);


		playersInLobby.remove((Integer) clientID); // removal by object
		readyPlayersInLobby.remove((Integer) clientID);
		checkAllReady(); // The player that left could be the last player that wasn't ready

		// Note: The color belonging to a client is not removed from the score even if the client disconnects

		// All users disconnected. Reset server state and restart server connection thread to allow new connections
		if(clientOutputs.size() == 0) {
			System.out.println("All client's disconnected. Resetting Server");
			resetServerState();
			NetworkServer server = new NetworkServer();
			server.setName("Server");
			server.setDaemon(true);
			server.start();
		}
	}

	private synchronized void resetServerState() {
		clientOutputs.clear();
		clientColors.clear();
		canvasesInUse.clear();
		clientScores.clear();
		playersInLobby.clear();
		readyPlayersInLobby.clear();
		lobbyMessagesList.clear();
		Arrays.fill(isLocked, false);
	}


	/**
	 * Locks the given canvas.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param canvasID The ID of the canvas to be locked.
	 */
	public synchronized void lockCanvas(int canvasID) {
		if(canvasID >= isLocked.length || canvasID < 0) {
			throw new IllegalArgumentException("Invalid canvasID");
		}

		isLocked[canvasID] = true;
	}

	/**
	 * A thread-safe method which attempts to acquire a canvas for drawing by a client.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client
	 * @param canvasID The ID of the canvas
	 * @return True if the client successfully acquired the canvas, false otherwise
	 */
	public synchronized boolean acquireCanvasForDrawing(int clientID, int canvasID) {
		if(!clientOutputs.containsKey(clientID)) {
			throw new IllegalArgumentException("Attempting to acquire a canvas with an invalid clientID");
		}

		// Check valid index
		if(canvasID >= isLocked.length || canvasID < 0) {
			throw new IllegalArgumentException("Invalid canvasID");
		}

		// If canvas is being drawn on by someone or is locked then it cannot be acquired
		if(canvasesInUse.containsValue(canvasID) || isLocked[canvasID]) {
			return false;
		}

		// otherwise acquire canvas for the client and return true
		canvasesInUse.put(clientID, canvasID);
		return true;
	}



	/**
	 * A thread-safe method which checks if a colour is registered by a client.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client, obtained by the hashcode of their socket
	 * @param colorHash The hashcode of the color being checked
	 * @return True if the client is allowed to use the given colour, false otherwise.
	 */
	public synchronized boolean checkRegisteredColor(int clientID, int colorHash) {
		return clientColors.containsKey(clientID) && clientColors.get(clientID).equals(colorHash);
	}

	/**
	 * A thread-safe method which checks if a client is allowed to draw on a given canvas.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client, obtained by the hashcode of their socket
	 * @param canvasID The ID of the canvas
	 * @return True if the client is allowed to draw on the canvas, false otherwise.
	 */
	public synchronized boolean checkValidCanvas(int clientID, int canvasID) {
		return canvasesInUse.containsKey(clientID) && canvasesInUse.get(clientID).equals(canvasID);
	}

	/**
	 * A method to send a message to the given client.
	 * <p>
	 * This should be the ONLY place where messages are ever sent to the client to allow thread-safety.
	 * @param message The message being sent
	 * @param clientID The id of the client to send the message to
	 */
	private synchronized void sendMessageToClient(String message, int clientID) {
		clientOutputs.get(clientID).println(message);
	}

	/**
	 * Sends a message to all connected clients.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param message The message being sent
	 */
	public synchronized void sendMessage(String message) {
		for(int clientID : clientOutputs.keySet()) {
			sendMessageToClient(message, clientID);
		}
	}

	/**
	 * Sends a message to the provided clientIDs
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param message The message being sent
	 * @param clientIDs An array containing the ids of the clients to send the message to.
	 */
	public synchronized void sendMessage(String message, int[] clientIDs) {
		for(int clientID : clientIDs) {
			if(!clientOutputs.containsKey(clientID)) {
				throw new IllegalArgumentException("Invalid clientID in array");
			}

			sendMessageToClient(message, clientID);
		}
	}

	/**
	 * Sends a message to the given client.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param message The message being sent
	 * @param clientID The ID of the client to send the message to
	 */
	public synchronized void sendMessage(String message, int clientID) {
		if(!clientOutputs.containsKey(clientID)) {
			throw new IllegalArgumentException("Invalid clientID in array");
		}

		sendMessageToClient(message, clientID);
	}

	/**
	 * Sends a message to all clients excluding the provided clientIDs.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param message The message being sent
	 * @param excludedClients An array containing the ids of the clients which the message should not be sent to.
	 */
	public synchronized void sendMessageExcluding(String message, List<Integer> excludedClients) {
		for(int clientID : clientOutputs.keySet()) {
			if(!excludedClients.contains(clientID)) {
				sendMessageToClient(message, clientID);
			}
		}
	}

	/**
	 * Sends a message to all clients excluding a single client.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param message The message being sent
	 * @param excludedClient The clientID of the client which the message should not be sent to.
	 */
	public synchronized void sendMessageExcluding(String message, int excludedClient) {
		for(int clientID : clientOutputs.keySet()) {
			if(clientID != excludedClient) {
				sendMessageToClient(message, clientID);
			}
		}
	}

	/**
	 * Releases the canvas acquired by the provided client. Does nothing if no canvases are acquired by the client.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client whose canvas is being released.
	 */
	public synchronized void releaseAcquiredCanvas(int clientID) {
		canvasesInUse.remove(clientID);
	}

	/**
	 * Attempts to register the given color with the given client.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client to register the color to.
	 * @param colorHash The hashcode of the color being registered
	 * @return True if the color has been successfully registered to the client, false otherwise.
	 */
	public synchronized boolean registerColor(int clientID, int colorHash) {
		if(clientColors.containsValue(colorHash)) {
			return false;
		}

		clientColors.put(clientID, colorHash);
		return true;
	}

	/**
	 * Sets the score of a given color.
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param color The color whose score is to be set
	 * @param score The new value of the score
	 * @return True if all canvases are locked, (i.e. fully coloured in) indicating the game has ended. False otherwise.
	 */
	public synchronized boolean setScore(Color color, int score){
		clientScores.put(color, score);
		return !Arrays.asList(isLocked).contains(false);
	}

	/**
	 * Gets the winning color. If there is a single winner this is the color belonging to the player.
	 * If there is a tie then the color is Color.Transparent.
	 * <p>
	 * This method should only be called when the game is completely finished (i.e. all canvases coloured in)
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @return The winning color.
	 */
	public synchronized Color getWinningColor() {
		if(Arrays.asList(isLocked).contains(false)) {
			throw new IllegalStateException("Attempting to get the winning color in an unfinished game");
		}

		int highestScore = getWinnerScore();
		int scoreCount = 0;
		Color color = null;

		for (Map.Entry<Color, Integer> entry : clientScores.entrySet()) {  // Iterate through hashmap
			if (entry.getValue() == highestScore) {
				scoreCount++;
				color = entry.getKey();
			}
		}

		return scoreCount == 1 ? color : Color.TRANSPARENT;
	}

	/**
	 * Gets the score of the winner. If there is a tie it returns the tied score.
	 * <p>
	 * This method should only be called when the game is completely finished (i.e. all canvases coloured in)
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @return The winning score
	 */
	public synchronized int getWinnerScore() {
		if(Arrays.asList(isLocked).contains(false)) {
			throw new IllegalStateException("Attempting to get the winning score in an unfinished game");
		}

		return Collections.max(clientScores.values());// This will return highest Score
	}


	/**
	 * Adds the client to the list of players that joined the lobby
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client
	 */
	public synchronized void lobbyPlayerJoined(int clientID) {
		playersInLobby.add(clientID);
	}

	/**
	 * Changes the lobby ready status of the client
	 * <p>
	 * This method is thread-safe and can be called by any thread without worrying about concurrency.
	 * @param clientID The ID of the client
	 * @param isReady The new ready status
	 */
	public synchronized void lobbyPlayerReady(int clientID, boolean isReady) {
		if(isReady) {
			if(!readyPlayersInLobby.contains(clientID)) {
				readyPlayersInLobby.add(clientID);
			}
		}
		else {
			readyPlayersInLobby.remove((Integer) clientID); // object removal
		}

		checkAllReady();
	}

	/**
	 * Adds the lobby message to the list of all lobby messages sent
	 * @param message The message to be added
	 */
	public synchronized void addLobbyMessageToList(String message) {
		lobbyMessagesList.add(message);
	}

	/**
	 * Sends the complete lobby message history to the provided client
	 * @param clientID The ID of the client to send the list to
	 */
	public synchronized void sendLobbyMessageHistory(int clientID) {
		for(String msg : lobbyMessagesList) {
			sendMessage(msg, clientID);
		}
	}

	/**
	 * Checks if all the players in the lobby are ready. If they are then it sends the lobby start countdown message
	 */
	private synchronized void checkAllReady() {
		if(playersInLobby.size() == readyPlayersInLobby.size()) {
			String message = NetworkMessage.generateLobbyStartCountdownMessage();
			addLobbyMessageToList(message);
			sendMessage(message);

			// Closing the server socket will prevent any new players from joining
			try {
				serverSocket.close();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Sets the ServerSocket that the game uses to create new connections.
	 * This must be set before any clients connect and the game starts.
	 * @param socket The ServerSocket
	 */
	public synchronized void setServerSocket(ServerSocket socket) {
		this.serverSocket = socket;
	}



	//checking if a player won a game or if there is a tie
	public ConcurrentHashMap<Color, Integer> checkResult(){
		int highestScore = Collections.max(clientScores.values());// This will return highest Score

		ConcurrentHashMap <Color, Integer> winners = new ConcurrentHashMap<>();

		for (Map.Entry<Color, Integer> entry : clientScores.entrySet()) {  // Iterate through hashmap
			if (entry.getValue() == highestScore) {
				winners.put(entry.getKey(), entry.getValue());
			}
		}
		return winners;
	}






}
