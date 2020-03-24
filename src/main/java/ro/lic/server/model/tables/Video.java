package ro.lic.server.model.tables;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;
import  java.util.Date;

@Entity
@Table(name = "Videos")
public class Video implements Serializable {
    //region Constructors
    protected Video(){

    }

    public Video(String name, User user, Date date) {
        this.name = name;
        this.user = user;
        this.date = date;
    }
    //endregion

    //region Table contents
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true,
        nullable = false)
    private String name;

    @JsonIgnore
    @ManyToOne
    private  transient User user;

    @Column(nullable = false)
    private Date date;
    //endregion

    //region Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    //endregion
}
