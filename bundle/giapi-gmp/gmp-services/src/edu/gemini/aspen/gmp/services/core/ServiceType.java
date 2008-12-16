package edu.gemini.aspen.gmp.services.core;

/**
 * The different type of services available
 */
public enum ServiceType {

    PROPERTY_SERVICE("Property Service"),
    LOGGING_SERVICE("Logging Service");
    
    private String _name;

    ServiceType(String name) {
        _name = name;
    }

    /**
     * Returns the diaplaly name associated
     * to the type.
     * @return the display name for the given service.
     */
    public String getName() {
        return _name;
    }

}
