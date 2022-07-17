package networking.server;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class ServerData {
	
	private static ServerData instance = null;
	
	public LinkedBlockingQueue<String> messageQueue;
	public List<PrintWriter> clientOutputs;
	
	public static ServerData getInstance() {
		if(instance == null) {
			instance = new ServerData();
		}
		
		return instance;
	}
	
	private ServerData() {
		messageQueue = new LinkedBlockingQueue<String>();
		clientOutputs = new ArrayList<PrintWriter>();
	}
}
