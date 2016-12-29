package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Command;
import cli.Shell;
import util.Config;
import util.Keys;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private ExecutorService threadPool;

	private Shell shell;
	private String host;
	private int tcpPort;
	private int udpPort;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;

	private boolean online;
	private String receiver;
	private String message;
	private DatagramSocket datagramSocket;
	private TcpListener clListener;
	private int port;
	private UdpListener udpListener;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		// DONE
		threadPool = Executors.newCachedThreadPool();
		online = false;
	}

	@Override
	public void run() {
		// DONE

		//Host name (or an IP address) where the chatserver is running
		host = config.getString("chatserver.host");

		//TCP port where the chatserver is listening for client connections
		tcpPort = config.getInt("chatserver.tcp.port");

		//UDP port where the chatserver is listening for client requests
		udpPort = config.getInt("chatserver.udp.port");

		//Register the bouncy castle provider
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		try {
			socket = new Socket(host, tcpPort);
			datagramSocket = new DatagramSocket();

			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			shell = new Shell(componentName, userRequestStream, userResponseStream);
			shell.register(this);	
			threadPool.submit(shell);

			clListener = new TcpListener(socket, in, out, userResponseStream, threadPool, this);
			threadPool.submit(clListener);

			userResponseStream.println("Client up and waiting for commands!");

		} catch (ConnectException e) {
			System.err.println("Connection was refused (host: " + host + ", port: " + tcpPort + "). " + e.getMessage());

		} catch (UnknownHostException e) {
			System.err.println("IP adress of the host could not be determinded (host: " + host + "). " + e.getMessage());

		} catch (SocketException e) {
			System.err.println("Error creating or acessing a socket. " + e.getMessage());

		} catch (IOException e) {
			System.err.println("Failed or interrupted I/O operation. " + e.getMessage());
		}
	}


	@Override
	@Command
	public String login(String username, String password) throws IOException {
		// DONE Auto-generated method stub

		if(!getOnline()) {
			out.println("!login " + username + " " + password);
		} else {
			userResponseStream.println("Already logged in.");
		}
		return null;
	}

	@Override
	@Command
	public String logout() throws IOException {
		// DONE Auto-generated method stub

		if(getOnline()) {
			out.println("!logout");
		} else {
			userResponseStream.println("Not logged in!");
		}
		return null;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		// DONE Auto-generated method stub

		if(getOnline()) {
			out.println("!send " + message);
		} else {
			userResponseStream.println("Not logged in!");
		}
		return null;
	}

	@Override
	@Command
	public String list() throws IOException {
		// DONE Auto-generated method stub

		String packet = "!list";
		byte[] buffer = packet.getBytes();
		DatagramPacket request = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(host), udpPort);
		datagramSocket.send(request);

		udpListener = new UdpListener(datagramSocket, userResponseStream, threadPool);
		threadPool.submit(udpListener);

		return null;
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		// DONE Auto-generated method stub

		if(getOnline()) {
			setMessage(message);
			setReceiver(username);
			out.println("!lookup private+ " + username);
		} else {
			userResponseStream.println("Not logged in!");
		}
		return null;
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		// DONE Auto-generated method stub

		if(getOnline()) {
			out.println("!lookup " + username);
		} else {
			userResponseStream.println("Not logged in!");
		}
		return null;
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		// DONE Auto-generated method stub

		if(getOnline()) {
			out.println("!register " + privateAddress);
			String[] p = privateAddress.split(":");
			setPort(Integer.parseInt(p[1]));
		} else {
			userResponseStream.println("Not logged in!");
		}
		return null;
	}

	@Override
	@Command
	public String lastMsg() throws IOException {
		// DONE Auto-generated method stub

		if(getOnline()) {
			out.println("!lastMsg");
		} else {
			userResponseStream.println("Not logged in!");
		}
		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		// DONE Auto-generated method stub

		if(getOnline()) {
			out.println("!logout");
		}
		userResponseStream.println("Exiting client.");

		if(shell != null) {
			shell.close();
		}

		if(socket != null && !socket.isClosed()) {
			socket.close();
		}
		if(out != null) {
			out.close();
		}
		if(in != null) {
			in.close();
		}
		if(userRequestStream != null) {
			userRequestStream.close();
		}
		if(userResponseStream != null) {
			userResponseStream.close();
		}
		if (clListener != null) {
			clListener.close();
		}
		if (udpListener != null) {
			udpListener.close();
		}

		threadPool.shutdown();

		return null;
	}

	public boolean getOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getPort() {
		return port;		
	}

	public void setPort(int port) {
		this.port = port;		
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		// DONE: start the client
		client.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	@Command
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub

		if(!getOnline()) {

			PrivateKey clientPrivKey;
			
			//Read the client's private key for the chatserver communication
			String keyDir = config.getString("keys.dir");
			try {
				clientPrivKey = Keys.readPrivatePEM(new File(keyDir + "/" + username + ".pem"));
				
			} catch (IOException e) {
				System.err.println("Failed to read the private key of " + username + "! " + e.getMessage());
			}
			
			//TODO: error if username not found
			
			// !authenticate <username> <client-challenge>
			//String challenge = "";
			//out.println("!authenticate " + username + challenge);
			//encode using base64 + encrypt using RSA
			
			//userResponseStream.println("Successfully read the user's key! " + username);
			
			
		} else {
			userResponseStream.println("Already logged in.");
		}

		return null;
	}

}
