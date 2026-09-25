package com.linepulse.auth;

public enum UserRole {
    ADMIN,
    TECHNICIAN,
    OPERATOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
