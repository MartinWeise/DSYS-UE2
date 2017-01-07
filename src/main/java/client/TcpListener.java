package client;

import org.bouncycastle.util.encoders.Base64;
import util.Keys;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.security.Key;
import javax.crypto.Mac;

public class TcpListener extends Thread {

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private PrintStream userResponseStream;
	private Client client;
	private boolean end;
	private boolean awaitingMsg;
	private ServerSocket serverSocket;
	private PrintWriter writer;
	private BufferedReader reader;
	private ExecutorService threadPool;
	private TcpMsgListener msgListener;
	private Socket cSocket;

	public TcpListener(Socket socket, BufferedReader in, PrintWriter out, PrintStream userResponseStream, ExecutorService threadPool, Client client) {
		this.socket = socket;
		this.in = in;
		this.out = out;
		this.userResponseStream = userResponseStream;
		this.threadPool = threadPool;
		this.client = client;
		this.end = false;
		this.awaitingMsg = false;
	}

	public void run() {

		try { 
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			while(!end && in != null) {

				String response = "";
				if (in != null && in.ready()) {
					response = in.readLine();
				}
				
				//Wait for incoming messages
				if(response != null && response.startsWith("Successfully registered address")) {
					msgListener = new TcpMsgListener(new ServerSocket(client.getPort()), userResponseStream, threadPool, this.client);
					threadPool.submit(msgListener);
				}

				if(response != null && response.startsWith("private+")) {
					String[] parts = response.split("\\s");

					if(parts[1].startsWith("Wrong")) {
						response = "Wrong username or user not reachable";
					} else {
						String adress = parts[1];
						String[] p = adress.split(":");
						String host = p[0];
						int port = Integer.parseInt(p[1]);
						response = "";
						
						//Open socket and try to send message to client
						try {
							cSocket = new Socket(host, port);
							writer = new PrintWriter(cSocket.getOutputStream(), true);						
							reader = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));

							//Michael started to code here
							String message = "!msg " + client.getMessage();
							File key = new File (client.getKey());
							Key secretKey = Keys.readSecretKey(key);
							Mac hMac = Mac.getInstance("HmacSHA256");
							hMac.init(secretKey);
							hMac.update(Byte.parseByte(message));
							byte[] hash = hMac.doFinal();
							byte[] encodedHash = Base64.encode(hash);

							message = encodedHash + message;
							writer.println(message);

							awaitingMsg = true;

							//TODO check hash of !ack or !tempered message
							if (reader != null) {
								String res = reader.readLine();
								int index = 0;
								System.err.println("Response: '" + res + "'.");
								if (res.contains("!tempered")) {
									index = res.indexOf("!tempered");
								} else if (res.contains("!ack")){
									index = res.indexOf("!ack");
								}

								response = res.substring(index);
								byte[] sentHash = res.substring(0,index).getBytes();
								hMac.update(Byte.parseByte(response));
								byte[] realHash = hMac.doFinal();
								realHash = Base64.encode(realHash);
								boolean tempered = MessageDigest.isEqual(sentHash, realHash);

								if (tempered) {
									System.out.println("The message received has been tempered with: " + response);
								}

								if(response.equals("!ack") || response.contains("!tempered")) {
									awaitingMsg = false;
									cSocket.close();
								}
								//Michael stopped coding here
							}

						} catch (ConnectException e) {
							System.err.println("Connection was refused (host: " + host + ", port: " + port + "). " + e.getMessage());

						} catch (UnknownHostException e) {
							System.err.println("IP adress of the host could not be determinded (host: " + host + "). " + e.getMessage());

						} catch (SocketException e) {
							System.err.println("Error creating or acessing a socket. " + e.getMessage());

						} catch (InvalidKeyException e) {
							e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						}
					}
				} else if (response != null && response.startsWith("!tinkered")) {
					out.println("Last message sent was manipulated");
				}
				
				if(awaitingMsg && reader != null) {
					String res = reader.readLine();
					if(res.equals("!ack")) {
						awaitingMsg = false;
						response = res;
						cSocket.close();
					}
				}

				if(response != null && response.equals("Server closed.")) {
					out.println("!logout");
					close();
				}
				if(response != null && !response.equals("")) {
					userResponseStream.println("Response: " + response);
				}
			}

		} catch (IOException e) {
			System.err.println("client tcp listener: " + e.getMessage());
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

		if(serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(cSocket != null && !cSocket.isClosed()) {
			try {
				cSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if(in != null) {
			try {
				in.close();
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
