package nameserver;

import java.rmi.Remote;
import java.rmi.RemoteException;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

/**
 * Please note that this interface is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public interface INameserverForChatserver extends Remote {

	public void registerUser(String username, String address)
			throws RemoteException, AlreadyRegisteredException,
			InvalidDomainException;

	public INameserverForChatserver getNameserver(String zone)
			throws RemoteException;

	public String lookup(String username) throws RemoteException;

}
