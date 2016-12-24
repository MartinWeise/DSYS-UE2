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
import java.util.Map;
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
public class Nameserver implements INameserver, INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private Shell shell;
	private Registry registry;
	private ConcurrentHashMap<String, INameserver> subzones;
	private ConcurrentHashMap<String, String> users;

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
			if (config.listKeys().contains("domain")) {
				/* component is non-root */
				registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
				INameserver root = (INameserver) registry.lookup(config.getString("root_id"));
				INameserver nameserverRemote = (INameserver) UnicastRemoteObject.exportObject(this, 0);
				root.registerNameserver(config.getString("domain"), nameserverRemote, nameserverRemote);
			} else {
				/* component is root */
				registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
				INameserver nameserverRemote = (INameserver) UnicastRemoteObject.exportObject(this, 0);
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

	/**
	 * Method for listing nameservers {@link ConcurrentHashMap<String,INameserver>} from view of the current one.
	 * @return List of registered nameservers
     */
	@Override
	@Command
	public String nameservers() {
		LinkedList<String> temp = new LinkedList<>();
		String out = "";
		int k = 1;
		/* build linked-list for sorting later */
		for (String nameserver : subzones.keySet()) {
			temp.add(nameserver);
		}
		/* sort entries A-Z */
		Collections.sort(temp);
		/* build output string */
		for (String zone : temp) {
			out += (k++) + ". " + zone + "\n";
		}
		return out;
	}

	@Override
	@Command
	public String addresses() {
		LinkedList<String> temp = new LinkedList<>();
		String out = "";
		int k = 1;
		/* prepare list for sorting later */
		for (Map.Entry<String, String> u : users.entrySet()) {
			temp.add(u.getKey() + " " + u.getValue());
		}
		/* sort */
		Collections.sort(temp);
		/* build output string */
		for (String line : temp) {
			out += (k++) + ". " + line + "\n";
		}
		return out;
	}

	@Override
	@Command
	public String exit() throws IOException {
		shell.close();
		if (!UnicastRemoteObject.unexportObject(this, false)) {
			/* now force it */
			if (!UnicastRemoteObject.unexportObject(this, true)) {
				throw new RuntimeException("Failed to unexport component " + componentName);
			}
		}
		return "Shutdown of " + componentName + " completed";
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

	@Override
	public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
		if (domain.matches("[A-z0-9.]+")) {
			/* domain is valid */
			if (domain.contains(".")) {
				/* domain has subzone */
				String newDomain = domain.substring(0, domain.lastIndexOf('.'));
				String nameServerDomain = domain.substring(domain.lastIndexOf('.') + 1);
				INameserver ns = subzones.get(nameServerDomain);
				if (ns != null) {
					ns.registerNameserver(newDomain, nameserver, nameserverForChatserver);
				}
			} else {
				subzones.put(domain, nameserver);
			}
		} else {
			throw new InvalidDomainException("Domain " + domain + " malformed.");
		}
	}

	@Override
	public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
		if (username.matches("[A-z0-9.]+")) {
			/* username is valid */
			if (username.contains(".")) {
				/* username has subzone */
				String usernameZone = username.substring(0, username.lastIndexOf('.'));
				String domainZone = username.substring(username.lastIndexOf('.') + 1);
				INameserver ns = subzones.get(domainZone);
				if (ns != null) {
					ns.registerUser(usernameZone, address);
				}
			} else {
				users.put(username, address);
			}
		} else {
			throw new InvalidDomainException("Username " + username + " malformed.");
		}
	}

	@Override
	public INameserverForChatserver getNameserver(String zone) throws RemoteException {
		if (subzones.containsKey(zone)) {
			return subzones.get(zone);
		} else {
			throw new RemoteException("Couldn't find nameserver " + zone + ".");
		}
	}

	@Override
	public String lookup(String username) throws RemoteException {
		if (users.containsKey(username)) {
			return users.get(username);
		} else {
			return username + " is not registered.";
		}
	}
}
