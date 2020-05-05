package ro.lic.server.model.non_db_models;

import com.google.gson.annotations.Expose;

public class UserLocation {
    @Expose
    private double lat;
    @Expose
    private double lng;

    public UserLocation(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLatLng(double lat, double lng){
        this.lat = lat;
        this.lng = lng;
    }
}
