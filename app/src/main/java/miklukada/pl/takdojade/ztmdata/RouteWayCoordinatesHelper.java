package miklukada.pl.takdojade.ztmdata;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

/**
 * Created by Barca on 2016-04-10.
 */
public class RouteWayCoordinatesHelper {

    private final String TAG = getClass().getSimpleName();

    private ArrayList<GeoPoint> geoPoints;

    private String seq;

    private String routeId;

    public RouteWayCoordinatesHelper(ArrayList<GeoPoint> geoPoints, String seq, String routeId) {
        this.geoPoints = geoPoints;
        this.seq = seq;
        this.routeId = routeId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public ArrayList<GeoPoint> getGeoPoints() {
        return geoPoints;
    }

    public void setGeoPoints(ArrayList<GeoPoint> geoPoints) {
        this.geoPoints = geoPoints;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }
}
