package client;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class TcpMsgListener extends Thread {

	private boolean end;
	private ServerSocket serverSocket;
	private PrintStream userResponseStream;
	private ExecutorService threadPool;
	private TcpMsgHandler handler;
	private List<TcpMsgHandler> h = Collections.synchronizedList(new ArrayList<TcpMsgHandler>());
	private Socket socket;

	
	public TcpMsgListener(ServerSocket serverSocket, PrintStream userResponseStream, ExecutorService threadPool) {
		this.serverSocket = serverSocket;
		this.userResponseStream = userResponseStream;
		this.threadPool = threadPool;
		this.end = false;
	}
	
	
	public void run() {
		
		while (!end) {

			socket = null;

			try {
				//Wait for client to connect
				socket = serverSocket.accept();
				System.err.println("Started server socket at " + socket.getInetAddress());

				//Handle incoming message in a separate thread
				handler = new TcpMsgHandler(socket, userResponseStream, this);
				threadPool.submit(handler);
				h.add(handler);

			} catch (IOException e) {
				System.err.println("msg listener: " + e.getMessage());
			}
		}
		
	}
	
	public void close() {
		end = true;
		
		for(TcpMsgHandler elem : h) {
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
