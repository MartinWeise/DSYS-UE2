package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cli.Command;
import cli.Shell;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private Shell shell;
	private NameserverConfig nameserver;
	private Registry registry;
	private ConcurrentHashMap<String, INameserver> subzones;

	/**
	 * @param componentName
	 *            the name of the componentant - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Nameserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		/* register shell */
		shell = new Shell(componentName, this.userRequestStream, this.userResponseStream);
		shell.register(this);
	}

	/**
	 * Register the root {@link INameserver} and bind the nameserverRemote {@link INameserver} to it
	 * Mostly from Oracle RMI docs
	 */
	@Override
	public void run() {
		try {
			subzones = new ConcurrentHashMap<>();
			nameserver = new NameserverConfig(subzones);
			if (config.listKeys().contains("domain")) {
				/* component is non-root */
				registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
				INameserver root = (INameserver) registry.lookup(config.getString("root_id"));
				INameserver nameserverRemote = (INameserver) UnicastRemoteObject.exportObject(nameserver, 0);
				root.registerNameserver(config.getString("domain"), nameserverRemote, nameserverRemote);
			} else {
				/* component is root */
				registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
				INameserver nameserverRemote = (INameserver) UnicastRemoteObject.exportObject(nameserver, 0);
				registry.bind(config.getString("root_id"), nameserverRemote);
			}
		} catch (RemoteException | AlreadyBoundException | NotBoundException
				| AlreadyRegisteredException | InvalidDomainException e) {
			throw new RuntimeException(e);
		}
		/* start shell */
		new Thread(shell).start();
		System.out.println("Component " + componentName + " up and running!");
	}

	@Override
	@Command
	public String nameservers() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Command
	public String addresses() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		// TODO: start the nameserver
		nameserver.run();
	}

}
