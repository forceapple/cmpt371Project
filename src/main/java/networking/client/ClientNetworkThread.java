package networking.client;

import java.io.IOException;
import java.io.*;
import java.util.List;

public class ClientNetworkThread extends Thread {
	
	private BufferedReader input;
	private List<NetworkObserver> observers;
	
	public ClientNetworkThread(BufferedReader input, List<NetworkObserver> observers) {
		this.input = input;
		this.observers = observers;
	}
	
	public void run() {
		String msg;
		
		try {
			while(true) {
				msg = input.readLine();
				for(NetworkObserver obs : observers) {
					obs.messageReceived(msg);
				}
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}

	}
	
}
