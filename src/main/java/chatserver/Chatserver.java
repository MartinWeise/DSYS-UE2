package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.security.Security;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Command;
import cli.Shell;
import util.Config;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private Shell shell;
	private int tcpPort;
	private int udpPort;
	private ConcurrentHashMap<String, UserData> users;
	private ExecutorService threadPool;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private TcpListener tcpListener;
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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		// DONE
		users = new ConcurrentHashMap<String, UserData>();
	}

	@Override
	public void run() {
		// DONE

		//Port to be used for instantiating a java.net.ServerSocket (handling TCP requests from clients)
		tcpPort = config.getInt("tcp.port");

		//Port to be used for instantiating a java.net.DatagramSocket (handling UDP requests from clients)
		udpPort = config.getInt("udp.port");

		//Register the bouncy castle provider
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		//Read the user properties and save username and password to the user's hashmap
		Config userProp = new Config("user");
		Set<String> keys = userProp.listKeys();

		for(String elem : keys) {
			String[] parts = elem.split(".password");
			String userName = parts[0];
			String userPW = userProp.getString(userName + ".password");

			UserData data = new UserData(userName, userPW);			
			users.putIfAbsent(userName, data);
		}

		//Create a thread every time a request is received by reusing already existing thread instances
		threadPool = Executors.newCachedThreadPool();

		try {

			serverSocket = new ServerSocket(tcpPort);
			datagramSocket = new DatagramSocket(udpPort);

			shell = new Shell(componentName, userRequestStream, userResponseStream);
			shell.register(this);

			//Start the shell
			threadPool.submit(shell);
			userResponseStream.println("Server up and waiting for commands!");

			//Listen for new connections
			tcpListener = new TcpListener(config, serverSocket, userResponseStream, users, threadPool);
			threadPool.submit(tcpListener);

			//Wait for incoming packets
			udpListener = new UdpListener(datagramSocket, userResponseStream, tcpListener, threadPool);
			threadPool.submit(udpListener);

		} catch (IOException e) {
			System.err.println("Failed or interrupted I/O operation. " + e.getMessage());
		}

	}

	@Override
	@Command
	public String users() throws IOException {
		// DONE Auto-generated method stub

		Map<String, UserData> u = tcpListener.getUsers();
		String out = "";

		for(Map.Entry<String, UserData> e : u.entrySet()){	
			out += e.getKey();
			if(e.getValue().getOnline()) {
				out += " online\n";
			} else {
				out += " offline\n"; 
			}
		}
		return out;
	}

	@Override
	@Command
	public String exit() throws IOException {
		// DONE Auto-generated method stub

		tcpListener.close();
		udpListener.close();

		if(shell != null) {
			shell.close();
		}
		if(serverSocket != null) {
			serverSocket.close();
		}
		if(datagramSocket != null) {
			datagramSocket.close();
		}
		if(userRequestStream != null) {
			userRequestStream.close();
		}
		if(userResponseStream != null) {
			userResponseStream.close();
		}
		threadPool.shutdown();

		return "Exiting chatserver.";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		// DONE: start the chatserver
		chatserver.run();
	}

}
