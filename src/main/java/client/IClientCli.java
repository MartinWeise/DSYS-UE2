package client;

import java.io.IOException;

public interface IClientCli {

	// --- Commands needed for Lab 1 ---

	/**
	 * Authenticates the client with the provided username and password.
	 *
	 * @param username
	 *            the name of the user
	 * @param password
	 *            the password
	 * @return status whether the authentication was successful or not
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String login(String username, String password) throws IOException;

	/**
	 * Performs a logout if necessary and closes open connections between client
	 * and chatserver.
	 *
	 * @return message stating whether the logout was successful
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String logout() throws IOException;

	/**
	 * Sends a public message to all users that are currently online.
	 *
	 * @param message
	 *            message to be sent to all online users
	 * 
	 * @return message stating whether the sending was successful
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String send(String message) throws IOException;

	/**
	 * Lists all online users. This command is the only command
	 * that does not require a logged in user. Additionally, this command is
	 * transmitted and received via UDP.
	 *
	 * @return a string containing all the known users.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String list() throws IOException;

	/**
	 * Sends a private message to the given user. In order to establish a
	 * private connection to the other user an implicit lookup has to be
	 * performed.
	 *
	 * @param username
	 *            user that should receive the private message
	 * @param message
	 *            message to be sent to all online users
	 * 
	 * @return message stating whether the sending was successful
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String msg(String username, String message) throws IOException;

	/**
	 * Performs a lookup of the given username and returns the address (IP:port)
	 * that has to be used to establish a private conversation.
	 *
	 * @param username
	 *            communication partner of private conversation.
	 * 
	 * @return a string containing the address (IP:port)
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String lookup(String username) throws IOException;

	/**
	 * Registers the private address (IP:port) that can be used by another user
	 * to establish a private conversation. Furthermore, the client creates a 
	 * new ServerSocket for the given port and listens for incoming connections 
	 * from other clients.
	 *
	 * @param privateAddress
	 *            address consisting of 'IP:port' that is used for creating a
	 *            TCP connection
	 * 
	 * @return message stating whether the registration was successful
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String register(String privateAddress) throws IOException;

	/**
	 * Prints the last received message, considering only public
	 * messages.
	 * 
	 * @return a string containing the last received message
	 * @throws IOException
	 */
	public String lastMsg() throws IOException;

	/**
	 * Performs a shutdown of the client and release all resources.<br/>
	 * Shutting down an already terminated client has no effect.
	 * <p/>
	 * Logout the user if necessary and be sure to releases all resources, stop
	 * all threads and close any open sockets.
	 *
	 * @return exit message
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String exit() throws IOException;

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	/**
	 * Authenticates the client with the provided username and key.
	 *
	 * @param username
	 *            the name of the user
	 * @return status whether the authentication was successful or not
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String authenticate(String username) throws IOException;

}
