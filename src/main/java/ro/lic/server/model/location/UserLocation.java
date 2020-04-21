package ro.lic.server.model.location;

public class UserLocation {
    private double lat;
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
