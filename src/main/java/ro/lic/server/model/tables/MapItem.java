package ro.lic.server.model.tables;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "MapItems")
public class MapItem implements Serializable {

    protected MapItem(){

    }

    public MapItem(String name, String description, int color, String type, List<Coordinates> coordinates) {
        //this.id = java.util.UUID.randomUUID().getLeastSignificantBits();
        this.name = name;
        this.description = description;
        this.color = color;
        this.type = type;
        this.coordinates = coordinates;

    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Expose
    private Long id;
    @Expose
    @Column(name = "Name",
            nullable = false)
    private String name;
    @Expose
    @Column(name = "Description",
            nullable = false)
    private String description;
    @Expose
    @Column(name = "Color",
            nullable = false)
    private int color;
    @Expose
    @Column(nullable = false)
    private String type;
    @Expose
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "coordinate_id")
    private List<Coordinates> coordinates;

    public Long getId() {
        return id;
    }

    //region Getters and setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Coordinates> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Coordinates> coordinates) {
        this.coordinates = coordinates;
    }

    //endregion
}
