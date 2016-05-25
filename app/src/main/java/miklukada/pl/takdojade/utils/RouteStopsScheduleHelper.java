package miklukada.pl.takdojade.utils;

import java.util.Date;

import miklukada.pl.takdojade.datamodel.Stop;


/**
 * Created by Barca on 2016-04-02.
 */
public class RouteStopsScheduleHelper {

    private final String TAG = getClass().getSimpleName();

    private Date date;
    private Stop stop;

    public RouteStopsScheduleHelper(Date date, Stop stop) {
        this.date = date;
        this.stop = stop;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }
}
