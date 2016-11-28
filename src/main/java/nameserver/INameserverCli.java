package nameserver;

import java.io.IOException;

/**
 * Please note that this interface is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public interface INameserverCli {

	// --- Commands needed for Lab 2 ---

	/**
	 * Prints out some information about each known nameserver (zones) from the
	 * perspective of this nameserver.<br/>
	 * 
	 * @return information about the nameservers
	 * @throws IOException
	 */
	public String nameservers() throws IOException;

	/**
	 * Prints out some information about each handled address, containing
	 * username and address (IP:port).<br/>
	 * 
	 * @return the address information
	 * @throws IOException
	 */
	public String addresses() throws IOException;

	/**
	 * Performs a shutdown of the nameserver and releases all resources. <br/>
	 * Shutting down an already terminated nameserver has no effect.
	 *
	 * @return any message indicating that the nameserver is going to terminate
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String exit() throws IOException;

}
