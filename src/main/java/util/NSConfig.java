package util;

public class NSConfig {

    String rootserver;
    String host;
    String name;
    String domain;
    int port;

    public NSConfig(String name, String rootserver, String host, int port, String domain) {
        this.name = name;
        this.rootserver = rootserver;
        this.host = host;
        this.port = port;
        this.domain = domain;
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

    public String toString() {
        return "Name: " + this.name + " Root: " + this.rootserver + " Address: " + this.host + ":" + this.port + " Domain: " + this.domain;
    }

}
