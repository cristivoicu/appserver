package ro.lic.server.websocket.utils.databasemodels;

import ro.lic.server.model.Role;

import java.io.Serializable;

public class PasswordAndRole implements Serializable {
    private String password;
    private Role role;

    protected PasswordAndRole(){

    }

    public PasswordAndRole(String password, Role role) {
        this.password = password;
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }
}
