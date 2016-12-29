package chatserver;

import util.Config;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpListener extends Thread {

	private ServerSocket serverSocket;
	private PrintStream userResponseStream;
	private ConcurrentHashMap<String, UserData> users;

	private List<TcpHandler> h = Collections.synchronizedList(new ArrayList<TcpHandler>());
	
	private boolean end;
	private ExecutorService threadPool;
	private TcpHandler handler;
	private Socket socket;
	private Config config;


	public TcpListener(Config config, ServerSocket serverSocket, PrintStream userResponseStream, ConcurrentHashMap<String, UserData> users, ExecutorService threadPool) {
		this.serverSocket = serverSocket;
		this.userResponseStream = userResponseStream;
		this.users = users;
		this.end = false;
		this.threadPool = threadPool;
		this.config = config;
	}

	public Map<String, UserData> getUsers() {		
		return new TreeMap<String, UserData>(users);
	}

	public List<TcpHandler> getHandlerList() {
		return h;
	}


	public void run() {

		threadPool = Executors.newCachedThreadPool();

		while (!end) {

			socket = null;

			try {
				//Wait for client to connect
				socket = serverSocket.accept();

				//Handle each client request in a separate thread
				handler = new TcpHandler(config, socket, userResponseStream, users, this);
				threadPool.submit(handler);
				h.add(handler);

			} catch (IOException e) {
				System.err.println("tcp listener: " + e.getMessage());
			}
		}

	}

	public void close() {

		end = true;
		
		for(TcpHandler elem : h) {
			elem.close();
		}

		if(serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if(socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(userResponseStream != null) {
			userResponseStream.close();
		}

		threadPool.shutdown();

	}


}
