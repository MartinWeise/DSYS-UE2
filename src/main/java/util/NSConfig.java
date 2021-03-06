package util;

import java.util.TreeMap;

public class NSConfig {

    String rootserver;
    String host;
    String name;
    String domain;
    private TreeMap<String, NSConfig> registry;
    int port;

    public NSConfig(String name, String rootserver, String host, int port, String domain) {
        this.name = name;
        this.rootserver = rootserver;
        this.host = host;
        this.port = port;
        this.domain = domain;
        this.registry = new TreeMap<>();
    }

    public String getRootserver() {
        return this.rootserver;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getName() {
        return this.name;
    }

    public String getDomain() {
        return this.domain;
    }

    public void register(String key, NSConfig nameserver) {
        this.registry.put(key, nameserver);
    }

    public TreeMap<String, NSConfig> getNameservers() {
        return this.registry;
    }

    public NSConfig getNameserver(String key) {
        return this.registry.get(key);
    }

    public String toString() {
        return "Name: " + this.name + " Root: " + this.rootserver + " Address: " + this.host + ":" + this.port + " Domain: " + this.domain;
    }

}
