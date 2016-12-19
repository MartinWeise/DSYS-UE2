package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.TreeMap;

import util.Config;
import util.NSConfig;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private TreeMap<String, NSConfig> registry;

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

		this.registry = new TreeMap<>();
		readConfig();
	}

	@Override
	public void run() {
		// TODO
	}

	@Override
	public String nameservers() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addresses() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	private void readConfig() {
		String[] server = new String[] {"ns-root", "ns-at", "ns-de", "ns-vienna-at"};
		for (String serverkey : server) {
			Config ns = new Config(serverkey);
			registry.put(serverkey, new NSConfig(serverkey, ns.getString("root_id"), ns.getString("registry.host"),
					ns.getInt("registry.port"), ns.listKeys().contains("domain") ? ns.getString("domain") : null));
		}
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
