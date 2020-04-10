package ro.lic.server.model.tables;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "Actions")
public class Action implements Serializable {
    protected Action(){}

    public Action(String description, Date date, User user) {
        this.description = description;
        this.date = date;
        this.user = user;
    }

    //region Table columns
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Expose
    @SerializedName("id")
    private long ID;

    @Expose
    @Column(nullable = false)
    private String description;

    @Expose
    @Column(nullable = false)
    private Date date;

    @JsonIgnore
    @ManyToOne
    private User user;
    //endregion

    //region Getters and setters

    public long getID() {
        return ID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    //endregion
}
