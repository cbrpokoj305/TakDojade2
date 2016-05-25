package miklukada.pl.takdojade.comparator;

import java.util.ArrayList;
import java.util.Comparator;

import miklukada.pl.takdojade.route.Schedule;

/**
 * Created by Konrad on 2016-05-04.
 */
public class ScheduleComparator implements Comparator<ArrayList<Schedule>> {

    private final String TAG = getClass().getSimpleName();

    @Override
    public int compare(ArrayList<Schedule> lhs, ArrayList<Schedule> rhs) {
        int count = 0;
        if (lhs.size() != rhs.size()) {
            return 0;
        } else {
            for (int i = 0; i < lhs.size(); i++) {
                if (lhs.get(i).getStop().getStopName().equals(rhs.get(i).getStop().getStopName())
                        && lhs.get(i).getTime().getDay() == rhs.get(i).getTime().getDay()
                        && lhs.get(i).getTime().getHours() == rhs.get(i).getTime().getHours()
                        && lhs.get(i).getTime().getMinutes() == rhs.get(i).getTime().getMinutes()) {
                    count++;
                }
            }
        }
        if (count == lhs.size())
            return 1;
        else
            return 0;
    }
}
