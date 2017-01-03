package client;

import org.bouncycastle.util.encoders.Base64;
import util.Keys;

import javax.crypto.Mac;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class TcpMsgHandler extends Thread {

	private Socket socket;
	private PrintStream userResponseStream;
	private BufferedReader reader;
	private PrintWriter writer;
	private boolean end;
	private Client client;



	public TcpMsgHandler(Socket socket, PrintStream userResponseStream, TcpMsgListener msgListener, Client client) {
		this.socket = socket;
		this.userResponseStream = userResponseStream;
		this.end = false;
		this.client = client;
	}


	public void run() {
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);

			String message;

			while (!end && (message = reader.readLine()) != null) {

				userResponseStream.println(message);
				boolean tempered = false;

				//TODO check message for tempering

				String response = "!ack";
				if (tempered) {
					response = "!tempered " + message;
				}

				File key = new File (client.getKey());
				Key secretKey = Keys.readSecretKey(key);
				Mac hMac = Mac.getInstance("HmacSHA256");
				hMac.init(secretKey);
				hMac.update(Byte.parseByte(response));
				byte[] hash = hMac.doFinal();
				byte[] encodedHash = Base64.encode(hash);

				response = encodedHash + response;
				writer.println(response);
			}

		} catch (IOException e) {
			System.err.println("msg handler: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
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
