package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpMsgHandler extends Thread {

	private Socket socket;
	private PrintStream userResponseStream;
	private BufferedReader reader;
	private PrintWriter writer;
	private boolean end;


	public TcpMsgHandler(Socket socket, PrintStream userResponseStream, TcpMsgListener msgListener) {
		this.socket = socket;
		this.userResponseStream = userResponseStream;
		this.end = false;
	}


	public void run() {
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);

			String message;

			while (!end && (message = reader.readLine()) != null) {

				// TODO: remove this type of messages?
				System.out.println("Got private message " + message);
				userResponseStream.println(message);
				
				String response = "!ack";
				writer.println(response);
			}

		} catch (IOException e) {
			System.err.println("msg handler: " + e.getMessage());
		}
		
	}

	
	public void close() {
		end = true;

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
		if(writer != null) {
			writer.close();
		}
		if(reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}


}
