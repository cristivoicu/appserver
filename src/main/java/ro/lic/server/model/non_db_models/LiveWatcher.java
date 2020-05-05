package ro.lic.server.model.non_db_models;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class LiveWatcher implements Serializable {
    @Expose
    private String name;
    @Expose
    private String username;

    public LiveWatcher(String name, String username) {
        this.name = name;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
