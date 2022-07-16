package server;

import java.io.*;
import java.util.*;



public class NetworkMain {

	public static void main(String[] args) throws IOException {
		Scanner in = new Scanner(System.in);
		NetworkServer server = new NetworkServer(7070);
		server.setDaemon(true);
		server.setName("Server");
		server.start();
		

		in.nextLine();
		
	}
	
}
