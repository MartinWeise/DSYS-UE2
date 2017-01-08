package chatserver;

import nameserver.INameserver;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;
import util.Keys;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.logging.Logger;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.util.encoders.Base64;

public class TcpHandler extends Thread {

	//private static Logger logger = Logger.getLogger(Chatserver.class.getName());

	private Socket socket;
	private PrintStream userResponseStream;
	private ConcurrentHashMap<String, UserData> users;
	private BufferedReader reader;
	private PrintWriter writer;
	private TcpListener tL;
	private UserData d;
	private boolean end;
	private boolean online;
	private String username;
	private Config config;
	private PrivateKey privKey;
	private String serverChallenge;
	private boolean awaitingMessage;
	private SecretKey key;
	private byte[] IV;

	public TcpHandler(Config config, Socket socket, PrintStream userResponseStream, ConcurrentHashMap<String, UserData> users, TcpListener tL) {
		this.socket = socket;
		this.userResponseStream = userResponseStream;
		this.users = users;
		this.tL = tL;
		this.end = false;
		this.config = config;
		this.online = false;

		//Read the chatservers private key for the client communication
		String key = config.getString("key");
		try {
			privKey = Keys.readPrivatePEM(new File(key));

		} catch (IOException e) {
			System.err.println("Failed to read the chatserver's private key! " + e.getMessage());
		}
	}


	public void run() {

		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);

			String request;

			while (!end && (request = reader.readLine()) != null) {
				
				userResponseStream.println("Client: " + request);
				String response = "";
				
				//Decode the message from the client
				byte[] decodedMessage = Base64.decode(request);				
				request = new String(decodedMessage, "UTF-8");

				String[] parts = request.split("\\s");
				
				
				 if (request.startsWith("!logout")) {
					if (parts.length > 1) {
						response = "Command doesn't require any arguments: !logout";
					} else {
						synchronized (d) {
							if (online) {
								d.setOnlineStatus(false);
								online = false;
								d.setPrivateAdress("");
								users.put(d.getUserName(), d);
								response = "Successfully logged out.";
							} else {
								response = "Not logged in.";
							}
						}
					}


				} else if (request.startsWith("!send")) {
					if (online) {
						String message = request.substring("!send".length() + 1);

						int i = 0;
						for (Map.Entry<String, UserData> e : users.entrySet()) {
							if (!e.getKey().equals(d.getUserName()) && e.getValue().getOnline()) {
								e.getValue().setLastReceivedMessage(message);
								e.getValue().setSenderName(d.getUserName());

								//Get handlers of the receivers and send them the message
								List<TcpHandler> h = tL.getHandlerList();
								synchronized (h) {
									for (TcpHandler handler : h) {
										if (handler.d.getUserName().equals(e.getValue().getUserName())) {
											handler.writer.println(d.getUserName() + " sends: " + message);
										}
									}
									i++;
								}
							}
						}
						if (i == 0) {
							response = "No other users online to get the message.";
						} else {
							response = "Successfully send the message to " + i + " users.";
						}
					} else {
						response = "Not logged in.";
					}


				} else if (request.startsWith("!register")) {
					if (online) {
						if (parts.length > 2) {
							response = "Command requires only one argument: !register <IP:port>";
						} else {
							if (!parts[1].contains(":")) {
								response = "Wrong address format: <IP:port>";
							} else {
								Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"),
										config.getInt("registry.port"));
								INameserver rootns = (INameserver) registry.lookup(config.getString("root_id"));
								rootns.registerUser(d.getUserName(), parts[1]);
								d.setPrivateAdress(parts[1]);
								response = "Successfully registered address for " + d.getUserName() + ".";
							}
						}
						System.err.println(response);

					} else {
						response = "Not logged in.";
					}

				} else if (request.startsWith("!lookup")) {
					if (online) {
						if (parts.length > 3 || (parts.length == 3 && !parts[1].equals("private+"))) {
							response = "Command requires only one argument: !lookup <username>";
						} else if (parts.length == 3) {
							/* implicite lookup */
							String name = parts[2];
							if (!users.containsKey(name)) {
								response = "private+ Wrong username or user not registered.";
							} else {
								/* valid arguments */
								Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
								INameserverForChatserver remotens = (INameserverForChatserver) registry.lookup(config.getString("root_id"));
								String[] userParts = request.split("\\.");
								/* begin from last, end at first */
								for (int i = userParts.length - 1; i >= 1; i--) {
									remotens = remotens.getNameserver(userParts[i]);
								}
								String address = remotens.lookup(userParts[0].substring(17, userParts[0].length()));
								response = "private+ " + address;
								userResponseStream.println("Resolved implicite lookup '" + address + "'");
							}
						} else {
							/* normal lookup */
							Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
							INameserverForChatserver remotens = (INameserverForChatserver) registry.lookup(config.getString("root_id"));
							String[] userParts = request.split("\\.");
							/* begin from last, end at first */
							for (int i = userParts.length - 1; i >= 1; i--) {
								System.out.println(i + ": " + userParts[i]);
								remotens = remotens.getNameserver(userParts[i]);
							}
							response = remotens.lookup(userParts[0].substring(8, userParts[0].length()));
						}
					} else {
						response = "Not logged in.";
					}


				} else if (request.startsWith("!lastMsg")) {
					if (online) {
						if (parts.length > 1) {
							response = "Command doesn't require any arguments: !lastMsg";
						} else {
							if (d.getLastReceivedMessage().equals("")) {
								response = "No message received!";
							} else {
								response = d.getSenderName() + ": " + d.getLastReceivedMessage();
							}
						}
					} else {
						response = "Not logged in.";
					}


				} else {

					if(awaitingMessage) {
						//Decrypt the message using AES
						Cipher cipher = null;
						String decryptedMessage = null;
						try {
							cipher = Cipher.getInstance("AES/CTR/NoPadding");
							cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV));
							decryptedMessage = new String(cipher.doFinal(decodedMessage), "UTF-8");

						} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
							System.err.println("Failed to decrypt the first message! " + e.getMessage());
						}

						//Check if the message matches the <chatserver-challenge>
						if(!new String(Base64.decode(decryptedMessage), "UTF-8").equals(new String(Base64.decode(serverChallenge), "UTF-8"))) {
							System.err.println("The received message doesn't match the <chatserver-challenge>");

						} else {
							awaitingMessage = false;
							if (users.containsKey(username)) {
								if(!online) {
									d = users.get(username);

									synchronized (d) {
										if(!d.getOnline()) {
											//Authentication of the user succeeded
											users.put(d.getUserName(), d);
											d.setOnlineStatus(true);
											online = true;

										} else {
											System.err.println("Authentication failed! User is online on another client!");
										}
									}
								} else {
									System.err.println("Authentication failed! Someone is already online on this client!");
								}
							} else {
								System.err.println("Authentication failed! Unknown user!");
							}
						}

					} else {
						//Decrypt the message using RSA
						Cipher cipher = null;
						String decryptedMessage = null;
						try {
							cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
							cipher.init(Cipher.DECRYPT_MODE, privKey);
							decryptedMessage = new String(cipher.doFinal(decodedMessage), "UTF-8");

						} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
							System.err.println("Failed to decrypt the first message! " + e.getMessage());
						}

						if (decryptedMessage.startsWith("!authenticate")) {
							parts = decryptedMessage.split("\\s");
							if (parts.length > 3) {
								response = "Too much arguments: !authenticate <username> <client-challenge>";

							} else {
								username = parts[1];
								String clientChallenge = parts[2];

								//Read the public key of the user
								PublicKey userPubKey = null;
								String keyDir = config.getString("keys.dir");
								try {
									userPubKey = Keys.readPublicPEM(new File(keyDir + File.separator + username + ".pub.pem"));

								} catch (IOException e) {
									System.err.println("Failed to read the public key of " + username + "! " + e.getMessage());
								}


								//Generate the chatserver-challenge as a 32-byte-secure-random-number
								SecureRandom secureRandom = new SecureRandom(); 
								final byte[] challenge = new byte[32]; 
								secureRandom.nextBytes(challenge); 

								//Encode the challenge separately using Base64
								serverChallenge = new String(Base64.encode(challenge), "UTF-8");

								//Generate the 256-bit-secret-key for AES
								key = null;
								try {
									KeyGenerator keyGen = KeyGenerator.getInstance("AES");
									keyGen.init(256);
									key = keyGen.generateKey();
								} catch (NoSuchAlgorithmException e) {
									System.err.println("Failed to generate the secret key for AES! " + e.getMessage());
								}

								//Encode the secret key separately using Base64
								String secretKey = new String(Base64.encode(key.getEncoded()), "UTF-8");

								//Generate the IV parameter as a 16-byte-secure-random-number
								secureRandom = new SecureRandom(); 
								IV = new byte[16]; 
								secureRandom.nextBytes(IV);

								//Encode the IV parameter separately using Base64
								String IVparam = new String(Base64.encode(IV), "UTF-8");

								//Prepare the response: !ok <client-challenge> <chatserver-challenge> <secret-key> <iv-parameter>
								String message = "!ok " + clientChallenge + " " + serverChallenge + " " + secretKey + " " + IVparam;

								//Encrypt the overall message using RSA initialized with the user's public key
								cipher = null;
								byte[] encryptedMessage = null;
								try {
									cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
									cipher.init(Cipher.ENCRYPT_MODE, userPubKey);
									encryptedMessage = cipher.doFinal(message.getBytes("UTF-8"));

								} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
									System.err.println("Failed to encrypt the second message! " + e.getMessage());
								}

								//Encode the overall ciphertext using Base64
								byte[] encodedCipher = Base64.encode(encryptedMessage);

								//Send the message to the user
								response = new String(encodedCipher, "UTF-8");						
								awaitingMessage = true;
							}

						}
					}
				}

				//Print the server response
				writer.println(response);
			}

		} catch (IOException | NotBoundException | InvalidDomainException | AlreadyRegisteredException e) {
			System.err.println("tcp handler: " + e.getMessage());
		}

	}


	public void close() {

		end = true;
		writer.println("Server closed.");

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

