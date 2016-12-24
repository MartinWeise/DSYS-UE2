package chatserver;

import nameserver.INameserver;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TcpHandler extends Thread {

	private Socket socket;
	private PrintStream userResponseStream;
	private ConcurrentHashMap<String, UserData> users;
	private BufferedReader reader;
	private PrintWriter writer;
	private TcpListener tL;
	private UserData d;
	private boolean end;
	private Config config;

	public TcpHandler(Config config, Socket socket, PrintStream userResponseStream, ConcurrentHashMap<String, UserData> users, TcpListener tL) {
		this.socket = socket;
		this.userResponseStream = userResponseStream;
		this.users = users;
		this.tL = tL;
		this.end = false;
		this.config = config;
	}


	public void run() {

		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);

			String request;

			while (!end && (request = reader.readLine()) != null) {

				userResponseStream.println("Client: " + request);
				String[] parts = request.split("\\s");
				String response = "Command not available!";

				if (request.startsWith("!login")) {
					if (parts.length > 3) {
						response = "Command requires only two arguments: !login <username> <password>";
					} else {
						String name = parts[1];
						String password = parts[2];

						if (users.containsKey(name)) {
							d = users.get(name);

							synchronized (d) {
								if (d.getOnline()) {
									response = "Already logged in.";
								} else {
									if (!d.getUserPW().equals(password)) {
										response = "Wrong username or password.";
									} else {
										d.setOnlineStatus(true);
										users.put(d.getUserName(), d); //Update the value in the map
										response = "Successfully logged in.";
									}
								}
							}

						} else {
							response = "Wrong username or password.";
						}
					}


				} else if (request.startsWith("!logout")) {
					if (parts.length > 1) {
						response = "Command doesn't require any arguments: !logout";
					} else {
						synchronized (d) {
							if (d.getOnline()) {
								d.setOnlineStatus(false);
								d.setPrivateAdress("");
								users.put(d.getUserName(), d);
								response = "Successfully logged out.";
							} else {
								response = "Not logged in.";
							}
						}
					}


				} else if (request.startsWith("!send")) {
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


				} else if (request.startsWith("!register")) {
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

				} else if (request.startsWith("!lookup")) {
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
								// TODO remove these type of info message?
								System.out.println(i + ": " + userParts[i]);
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


				} else if (request.startsWith("!lastMsg")) {
					if (parts.length > 1) {
						response = "Command doesn't require any arguments: !lastMsg";
					} else {
						if (d.getLastReceivedMessage().equals("")) {
							response = "No message received!";
						} else {
							response = d.getSenderName() + ": " + d.getLastReceivedMessage();
						}
					}
				}

				//Print the server response
				writer.println(response);
			}
		} catch (IOException | NotBoundException | InvalidDomainException | AlreadyRegisteredException e) { //
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

