package networking.server;

import java.io.*;

public class ServerOutputThread extends Thread{
	
	private ServerData server;
	
	public ServerOutputThread() {
		server = ServerData.getInstance();
	}
	
	public void run() {
		System.out.println("Server Output Thread Starting");
		String msg;
		while(true) {
			try {
				msg = server.messageQueue.take();
				
				for(PrintWriter output : server.clientOutputs) {
					output.println(msg);
				}
			}
			catch(InterruptedException ex) {
				ex.printStackTrace();
			}

		}
	}
	
	

}
