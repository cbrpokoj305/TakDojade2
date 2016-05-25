package miklukada.pl.takdojade.route;

import java.util.Date;

import miklukada.pl.takdojade.datamodel.Stop;

/**
 * Created by Konrad on 2016-05-04.
 */
public class Schedule {

    private final String TAG = getClass().getSimpleName();

    private Date time;

    private Stop stop;

    public Schedule(Date time, Stop stop) {
        this.time = time;
        this.stop = stop;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }
}
