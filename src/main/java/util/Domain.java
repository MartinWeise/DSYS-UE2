package util;

import java.util.Arrays;

public class Domain {

    private String domain;

    public Domain(String domain) {
        this.domain = domain;
    }

    /**
     * Validate if the domain is a valid one.
     * @return result, true = valid domain, false otherwise
     */
    public boolean isValid() {
        return domain.matches("[A-z0-9.]+");
    }

    /**
     * Get the zone of a domain.
     * Example: sub.vienna.at ~> vienna
     * @return zone of a domain
     */
    public String getZone() {
        String[] part = domain.split("\\.");
        return part[part.length - 1];
    }

    /**
     * Validate if the domain has a subdomain.
     * Example: vienna.at
     * @return result, true = has subdomain, false otherwise
     */
    public boolean hasSubdomain() {
        return domain.contains("s");
    }

    /**
     * Get the subdomain of a domain.
     * @return subdomain
     */
    public String getSubdomain() {
        return domain.substring(0, domain.lastIndexOf("."));
    }

    public String toString() {
        return this.domain;
    }

}
