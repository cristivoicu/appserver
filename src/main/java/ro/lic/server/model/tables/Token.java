package ro.lic.server.model.tables;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "Tokens")
public class Token implements Serializable {
    //region Constructors
    protected Token(){

    }

    public Token(User user, String value, Date validUntil) {
        this.user = user;
        this.value = value;
        this.validUntil = validUntil;
    }
    //endregion

    //region Table contents
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne
    private User user;

    @Column(nullable = false,
            unique = true)
    private String value;

    @Column(nullable = false)
    private Date validUntil;
    //endregion

    //region Getters and setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }
    //endregion
}


