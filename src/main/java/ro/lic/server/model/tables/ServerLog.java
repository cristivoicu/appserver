package ro.lic.server.model.tables;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ro.lic.server.model.enums.Importance;
import ro.lic.server.model.enums.ServerLogActionType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "ServerLog")
public class ServerLog implements Serializable {
    protected ServerLog(){}

    public ServerLog(Date datetime,
                     User user,
                     String description,
                     Importance importance,
                     ServerLogActionType serverLogActionType) {
        this.datetime = datetime;
        this.user = user;
        this.description = description;
        this.importance = importance.name();
        this.serverLogActionType = serverLogActionType.name();
        this.username = user.getUsername();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Expose
    @SerializedName("id")
    private Long id;

    @Expose
    @Column(nullable = false)
    private Date datetime;

    @JsonIgnore
    @ManyToOne
    private User user;

    @Expose
    @Column(nullable = false)
    private String importance;

    @Expose
    @Column(nullable = false, length = 300)
    private String description;

    @Expose
    @Column(nullable = false,
            name = "ActionType")
    private String serverLogActionType;

    @Expose
    private String username;

    // region Getters and setters
    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public String getServerLogActionType() {
        return serverLogActionType;
    }

    public void setServerLogActionType(String serverLogActionType) {
        this.serverLogActionType = serverLogActionType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    //endregion
}
