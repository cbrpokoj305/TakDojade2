package miklukada.pl.takdojade.datamodel;

/**
 * Created by Konrad on 2016-02-26.
 */
public class Stop {

    public static final int STOP_DEFAULT = 0;

    public static final int STOP_ON_ROUTE = 1;

    public static final int STOP_NEAREST = 2;

    private String stopName;

    private String stopId;

    private double lat;

    private double lon;

    private String sequence;

    private String streetName;

    private int[] stopType;

    private int stop_status = STOP_DEFAULT;

    public int getStop_status() {
        return stop_status;
    }

    public void setStop_status(int stop_status) {
        this.stop_status = stop_status;
    }

    public int[] getStopType() {
        return stopType;
    }

    public void setStopType(int[] stopType) {
        this.stopType = stopType;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
}
