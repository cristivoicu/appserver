package ro.lic.server.model.tables;

import com.google.gson.annotations.Expose;

import javax.persistence.*;
import java.io.Serializable;

@Entity(name = "Coordinates")
@Table(name = "Coordinates")
public class Coordinates implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column
    @Expose
    private double latitude = 0.0;
    @Column
    @Expose
    private double longitude = 0.0;

    protected Coordinates(){

    }

    public Coordinates(long id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public Long getId() {
        return id;
    }

    public double getLongitude() {
        return longitude;
    }
}
