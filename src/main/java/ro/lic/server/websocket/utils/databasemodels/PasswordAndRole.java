package ro.lic.server.websocket.utils.databasemodels;

import ro.lic.server.model.Roles;

import java.io.Serializable;

public class PasswordAndRole implements Serializable {
    private String password;
    private Roles role;

    protected PasswordAndRole(){

    }

    public PasswordAndRole(String password, Roles role) {
        this.password = password;
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public Roles getRole() {
        return role;
    }
}
