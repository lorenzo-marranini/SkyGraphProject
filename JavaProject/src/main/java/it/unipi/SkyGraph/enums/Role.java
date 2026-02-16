package it.unipi.SkyGraph.enums;

public enum Role {
    AIRLINE_REPRESENTATIVE,
    TRAFFIC_CONTROLLER,
    REGISTERED_USER,
    ADMIN;
    public static Role fromString(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

}
