package networking.server;

import com.example.javafxtest.DrawInfo;
import com.example.javafxtest.LobbyPlayer;
import javafx.scene.paint.Color;
import networking.NetworkMessage;

import java.io.*;
import java.net.*;
import java.util.Objects;

/**
 * ClientThread is the thread that is created for every new client that connects to the server.
 * It handles all the messages sent to the server from the client and is also responsible for sending responses
 */
public class ClientThread extends Thread {
	private final Socket socket;
	private final ServerData server;
	private final int clientID;
	private BufferedReader input;

	private LobbyPlayer player = null;

	public ClientThread(Socket socket) {
		this.socket = socket;
		server = ServerData.getInstance();
		clientID = socket.hashCode();
	}

	public void run() {
		System.out.println("Client Thread Starting");
		PrintWriter output = null;
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);

			server.addClient(clientID, output);

			// It is very intentional to not leave this while loop unless an exception occurs.
			//noinspection InfiniteLoopStatement
			while(true) {
				// input.readLine is a blocking method so this thread will wait here until it receives an input
				processMessage(input.readLine());
			}
		}
		// SocketException should mean that the client disconnected
		catch (SocketException ex) {
			server.removeClient(clientID);
			sendLobbyPlayerLeft(); // Send disconnect message if client disconnected
			System.out.println("Client Disconnected (" + socket.getInetAddress().toString()
					+ ":" + socket.getPort() + ")");

			// I'm not completely sure if these need to be closed, but it shouldn't hurt to explicitly close them
			try {
				if (output != null) {
					output.close();
				}

				socket.close();
				input.close();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		catch (IOException ex) {
			System.out.println("Exception on Server: " + ex.getMessage());
			ex.printStackTrace();
		}

		System.out.println("Client Thread Stopping");
	}

	private void sendLobbyPlayerLeft() {
		String message = NetworkMessage.generateLobbyPlayerLeftMessage(player);
		server.addLobbyMessageToList(message);
		if(Objects.nonNull(player)) {
			server.sendMessage(message);
		}
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
				processLockMessage(data);
				break;
			case NetworkMessage.CANVAS_CLEAR:
				processCanvasClearMessage(data);
				break;
			case NetworkMessage.CANVAS_OWN:
				processCanvasOwnMessage(data);
				break;
			case NetworkMessage.CALCULATE_SCORE_AND_GET_RESULTS:
				processCalculateScoreRequest(data);
				break;
			case NetworkMessage.LOBBY_PLAYER_JOIN_HEADER:
				processPlayerJoinMessage(data);
				break;
			case NetworkMessage.LOBBY_PLAYER_READY_HEADER:
				processPlayerReadyMessage(data);
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

		// Check the colour and canvas are valid
		if(!server.checkValidCanvas(clientID, canvasID)) {
			throw new IllegalStateException("Attempting to draw on an canvas that isn't registered to the user");
		}
		if(!server.checkRegisteredColor(clientID, colorHash)) {
			// TODO: Implement sending errors to the client
			throw new IllegalStateException("Attempting to draw with an unregistered colour!");
		}

		server.sendMessageExcluding(NetworkMessage.addDrawMessageHeader(data), clientID);
	}

	private void processCanvasRequest(String data) {
		int canvasID = Integer.parseInt(data);

		boolean success = server.acquireCanvasForDrawing(clientID, canvasID);

		server.sendMessage(NetworkMessage.addCanvasRequestHeader(Boolean.toString(success)), clientID);
	}

	private void processCanvasRelease() {
		server.releaseAcquiredCanvas(clientID);
	}

	private void processColorRequest(String data) {
		int colorHash = Integer.parseInt(data);

		boolean success = server.registerColor(clientID, colorHash);
		server.sendMessage(NetworkMessage.addColorRequestHeader(Boolean.toString(success)), clientID);
	}

	private void processLockMessage(String data) {
		int canvasID = Integer.parseInt(data);
		server.lockCanvas(canvasID);
	}

	private void processCanvasClearMessage(String data) {
		server.sendMessage(NetworkMessage.addCanvasClearRequestHeader(data));
	}

	private void processCanvasOwnMessage(String data) {
		String id = data.split("/", 2)[0];
		String stringColor = data.split("/", 2)[1];
		Color color = Color.valueOf(stringColor);

		server.sendMessage(NetworkMessage.addCanvasOwnRequestHeader(id, color));
	}

	/*This method calls the method to store the score of the user in a hashmap and if game ended calls the method
	  to check the winner
	 */
	private void processCalculateScoreRequest(String data) {
		String stringScore = data.split("/")[0];
		String stringColor = data.split("/")[1];

		Color color = Color.valueOf(stringColor);
		int score = Integer.parseInt(stringScore);
		boolean allCanvasColored = server.setScore(color, score);

		//if every canvas is coloured then check winner
		if (allCanvasColored) {
			// passing the information to clients after checking results i.e. if player won a game or there is a tie
			server.sendMessage(NetworkMessage.generateScoresAndGameResults(Integer.toString(server.getWinnerScore()), server.getWinningColor()));
		}
	}

	private void processPlayerJoinMessage(String data) {
		String[] fields = data.split("/");
		player = new LobbyPlayer(Color.valueOf(fields[0]), fields[1]);
		String message = NetworkMessage.addLobbyPlayerJoinHeader(data);


		// Send message to self, then send history to self, then add to history, then send to everyone else
		// A message must always be added to history before being sent to everyone
		// this prevents someone from joining at the perfect time and missing the message since it was sent to everyone (they didn't join yet)
		// then they asked for history and got it (message wasn't in history) then finally the message got added to history but they missed it.
		// The self entry needs to be the first one the new player gets so it needs to be sent before history leading to this setup.
		server.sendMessage(message, clientID);
		server.sendLobbyMessageHistory(clientID);
		server.addLobbyMessageToList(message);
		server.sendMessageExcluding(message, clientID);

		server.lobbyPlayerJoined(clientID);
	}

	private void processPlayerReadyMessage(String data) {
		server.lobbyPlayerReady(clientID, Boolean.parseBoolean(data.split("/")[1]));
		String message = NetworkMessage.addLobbyPlayerReadyHeader(data);
		server.addLobbyMessageToList(message);
		server.sendMessage(message);
	}

}


