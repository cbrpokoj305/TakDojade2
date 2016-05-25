package miklukada.pl.takdojade.datamodel;

import java.util.ArrayList;

import miklukada.pl.takdojade.utils.RouteStopsScheduleHelper;
import miklukada.pl.takdojade.wktdata.WKTLinestring;


/**
 * Created by Konrad on 2016-02-26.
 */
public class Route {

    private String routeId;

    private String tripId;

    private String timeOfArrival;

    private ArrayList<String> times = new ArrayList<>();

    private ArrayList<String> intervals;

    private ArrayList<RouteStopsScheduleHelper> routeStopsScheduleHelpers = new ArrayList<>();

    private ArrayList<WKTLinestring> calculatedPoints = new ArrayList<>();

    private ArrayList<Street> streetsList = new ArrayList<>();

    private ArrayList<Stop> stopsList = new ArrayList<>();

    private int routeType;

    public int getRouteType() {
        return routeType;
    }

    public void setRouteType(int routeType) {
        this.routeType = routeType;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public ArrayList<Street> getStreetsList() {
        return streetsList;
    }

    public void setStreetsList(ArrayList<Street> streetsList) {
        this.streetsList = streetsList;
    }

    public ArrayList<WKTLinestring> getCalculatedPoints() {
        return calculatedPoints;
    }

    public void setCalculatedPoints(ArrayList<WKTLinestring> calculatedPoints) {
        this.calculatedPoints = calculatedPoints;
    }

    public ArrayList<Stop> getStopsList() {
        return stopsList;
    }

    public void setStopsList(ArrayList<Stop> stopsList) {
        this.stopsList = stopsList;
    }

    public String getTimeOfArrival() {
        return timeOfArrival;
    }

    public void setTimeOfArrival(String timeOfArrival) {
        this.timeOfArrival = timeOfArrival;
    }

    public ArrayList<String> getIntervals() {
        return intervals;
    }

    public void setIntervals(ArrayList<String> intervals) {
        this.intervals = intervals;
    }

    public ArrayList<RouteStopsScheduleHelper> getRouteStopsScheduleHelpers() {
        return routeStopsScheduleHelpers;
    }

    public void setRouteStopsScheduleHelpers(ArrayList<RouteStopsScheduleHelper> routeStopsScheduleHelpers) {
        this.routeStopsScheduleHelpers = routeStopsScheduleHelpers;
    }

    public ArrayList<String> getTimes() {
        return times;
    }

    public void setTimes(ArrayList<String> times) {
        this.times = times;
    }
}
