package nameserver;

import nameserver.INameserver;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Domain;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;


public class NameserverConfig implements INameserver {

    private ConcurrentHashMap<String, INameserver> subzones;

    public NameserverConfig(ConcurrentHashMap<String, INameserver> subzones) {
        this.subzones = subzones;
    }

    @Override
    public void registerNameserver(String domain, INameserver nameserver,INameserverForChatserver nameserverForChatserver)
            throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        Domain d = new Domain(domain);
        if (d.isValid()) {
            if (subzones.containsKey(d.getZone())) {
                /* Zone is already registered */
                throw new AlreadyRegisteredException("Domain " + d + " already registered.");
            } else if (d.hasSubdomain()) {
                /* Register the subdomain */
                subzones.get(d.getZone()).registerNameserver(d.getSubdomain(), nameserver, nameserverForChatserver);
            } else {
                /* Domain is a Top-Level one */
                subzones.put(d.getZone(), nameserver);
            }
            System.err.println("Registering Zone " + d.getZone());
        } else {
            throw new InvalidDomainException("Domain " + d + " malformed.");
        }
    }

    @Override
    public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException,
            InvalidDomainException {

    }

    @Override
    public INameserverForChatserver getNameserver(String zone) throws RemoteException {
        return null;
    }

    @Override
    public String lookup(String username) throws RemoteException {
        return null;
    }
}
