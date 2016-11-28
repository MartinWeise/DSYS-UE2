package test;

import nameserver.INameserverCli;
import nameserver.Nameserver;
import util.Config;
import util.TestInputStream;
import util.TestOutputStream;
import chatserver.Chatserver;
import chatserver.IChatserverCli;
import client.Client;
import client.IClientCli;

/**
 * Provides methods for starting an arbitrary amount of various components.
 */
public class ComponentFactory {
	/**
	 * Creates and starts a new client instance using the provided
	 * {@link Config} and I/O streams.
	 *
	 * @param componentName
	 *            the name of the component to create
	 * @return the created component after starting it successfully
	 * @throws Exception
	 *             if an exception occurs
	 */
	public IClientCli createClient(String componentName, TestInputStream in,
			TestOutputStream out) throws Exception {
		/*
		 * TODO: Here you can do anything in order to construct a client
		 * instance. Depending on your code you might want to modify the
		 * following lines but you do not have to.
		 */
		Config config = new Config("client");
		return new Client(componentName, config, in, out);
	}

	/**
	 * Creates and starts a new chatserver instance using the provided
	 * {@link Config} and I/O streams.
	 *
	 * @param componentName
	 *            the name of the component to create
	 * @return the created component after starting it successfully
	 * @throws Exception
	 *             if an exception occurs
	 */
	public IChatserverCli createChatserver(String componentName,
			TestInputStream in, TestOutputStream out) throws Exception {
		/*
		 * TODO: Here you can do anything in order to construct a chatserver
		 * instance. Depending on your code you might want to modify the
		 * following lines but you do not have to.
		 */
		Config config = new Config("chatserver");
		return new Chatserver(componentName, config, in, out);
	}

	// --- Methods needed for Lab 2. Please note that you do not have to
	// use them for the first submission. ---

	/**
	 * Creates and starts a new nameserver instance using the provided
	 * {@link Config} and I/O streams.
	 *
	 * @param componentName
	 *            the name of the component to create
	 * @return the created component after starting it successfully
	 * @throws Exception
	 *             if an exception occurs
	 */
	public INameserverCli createNameserver(String componentName, TestInputStream in,
			TestOutputStream out) throws Exception {
		/*
		 * TODO: Here you can do anything in order to construct a nameserver
		 * instance. Depending on your code you might want to modify the
		 * following lines but you do not have to.
		 */
		Config config = new Config(componentName);
		return new Nameserver(componentName, config, in, out);
	}
}
