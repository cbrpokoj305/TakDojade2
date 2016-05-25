package miklukada.pl.takdojade.comparator;

import java.util.Comparator;
import java.util.Date;

import miklukada.pl.takdojade.datamodel.Route;


/**
 * Created by Barca on 2016-04-02.
 */
public class TimeComparatorForBuses implements Comparator<Route> {


    @Override
    public int compare(Route lhs, Route rhs) {

        String time1 = lhs.getTimeOfArrival().substring(lhs.getTimeOfArrival().indexOf("T")+1,lhs.getTimeOfArrival().length()-3);
        String[] timeHelper1 = time1.split(":");
        String hour = timeHelper1[0];
        String min = timeHelper1[1];
        Date date1 = new Date();
        date1.setHours(Integer.parseInt(hour));
        date1.setMinutes(Integer.parseInt(min));

        String time2 = rhs.getTimeOfArrival().substring(rhs.getTimeOfArrival().indexOf("T")+1,rhs.getTimeOfArrival().length()-3);
        String[] timeHelper2 = time2.split(":");
        String hour2 = timeHelper2[0];
        String min2 = timeHelper2[1];
        Date date2 = new Date();
        date2.setHours(Integer.parseInt(hour2));
        date2.setMinutes(Integer.parseInt(min2));

        return date1.compareTo(date2);
    }
}
