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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

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

		out.println("!login " + username + " " + password);

		return null;
	}

	@Override
	@Command
	public String logout() throws IOException {
		// DONE Auto-generated method stub

		out.println("!logout");

		return null;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		// DONE Auto-generated method stub

		out.println("!send " + message);

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

		setMessage(message);
		setReceiver(username);
		out.println("!lookup private+ " + username);

		return null;
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		// DONE Auto-generated method stub

		out.println("!lookup " + username);

		return null;
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		// DONE Auto-generated method stub

		out.println("!register " + privateAddress);
		String[] p = privateAddress.split(":");
		setPort(Integer.parseInt(p[1]));

		return null;
	}

	@Override
	@Command
	public String lastMsg() throws IOException {
		// DONE Auto-generated method stub

		out.println("!lastMsg");

		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		// DONE Auto-generated method stub

		out.println("!logout");

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

	public String getKey() {
		return config.getString("hmac.key");
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

		PrivateKey userPrivKey = null;
		PublicKey serverPubKey = null;
		boolean error = false;

		//Read the private key of the user for the chatserver communication
		String keyDir = config.getString("keys.dir");
		try {
			userPrivKey = Keys.readPrivatePEM(new File(keyDir + "/" + username + ".pem"));

		} catch (IOException e) {
			System.err.println("Failed to read the private key of " + username + "! " + e.getMessage());
			error = true;
		}

		if(!error) {
			//Read the public key of the server
			String key = config.getString("chatserver.key");
			try {
				serverPubKey = Keys.readPublicPEM(new File(key));
			} catch (IOException e) {
				System.err.println("Failed to read the chatserver's public key! " + e.getMessage());
			}

			//Generate the client-challenge as a 32-byte-secure-random-number
			SecureRandom secureRandom = new SecureRandom(); 
			final byte[] challenge = new byte[32]; 
			secureRandom.nextBytes(challenge); 

			//Encode the challenge separately using Base64
			String encodedChallenge = new String(Base64.encode(challenge), "UTF-8");

			//Prepare the message: !authenticate <username> <client-challenge>
			String message = "!authenticate " + username + " " + encodedChallenge;

			//Encrypt the overall message using RSA initialized with the chatserver�s public key
			Cipher cipher = null;
			byte[] encryptedMessage = null;
			try {
				cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, serverPubKey);
				encryptedMessage = cipher.doFinal(message.getBytes("UTF-8"));

			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
				System.err.println("Failed to encrypt the first message! " + e.getMessage());
			}

			//Encode the overall ciphertext using Base64
			byte[] encodedCipher = Base64.encode(encryptedMessage);

			//Send the message to the chatserver
			out.println(new String(encodedCipher, "UTF-8"));


			//Get the server response
			String response = "";
			if(in != null) {
				response = in.readLine();
			}

			//Decode the message
			byte[] decodedMessage = Base64.decode(response);

			//Decrypt the message
			cipher = null;
			String decryptedMessage = null;
			try {
				cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
				cipher.init(Cipher.DECRYPT_MODE, userPrivKey);
				decryptedMessage = new String(cipher.doFinal(decodedMessage), "UTF-8");

			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
				System.err.println("Failed to decrypt the second message! " + e.getMessage());
			}

			if (decryptedMessage.startsWith("!ok")) {
				String[] parts = decryptedMessage.split("\\s");
				String clientChallenge = parts[1];
				String chatserverChallenge = parts[2];
				String secretKey = parts[3];
				String IVparam = parts[4];

				//Check if the received <client-challenge> matches the sent one
				if(!new String(Base64.decode(encodedChallenge), "UTF-8").equals(new String(Base64.decode(clientChallenge), "UTF-8"))) {
					System.err.println("The received <client-challenge> doesn't match the sent one!");

				} else {
					//Decode the secret-key and IV-paramter
					byte[] decSecretKey = Base64.decode(secretKey);
					SecretKey sKey = new SecretKeySpec(decSecretKey, 0, decSecretKey.length, "AES");
					byte[] decIV = Base64.decode(IVparam);

					//Encrypt the <chatserver-challenge> using AES initialized with the <secret-key> and the <iv-parameter>
					cipher = null;
					encryptedMessage = null;
					try {
						cipher = Cipher.getInstance("AES/CTR/NoPadding");
						cipher.init(Cipher.ENCRYPT_MODE, sKey, new IvParameterSpec(decIV));
						encryptedMessage = cipher.doFinal(chatserverChallenge.getBytes("UTF-8"));

					} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
						System.err.println("Failed to encrypt the third message! " + e.getMessage());
					}

					//Encode the ciphertext using Base64
					encodedCipher = Base64.encode(encryptedMessage);

					//Send the message to the chatserver
					out.println(new String(encodedCipher, "UTF-8"));

				}
			}
		}
		return null;
	}

}
