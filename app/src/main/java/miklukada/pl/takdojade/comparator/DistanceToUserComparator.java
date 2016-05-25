package miklukada.pl.takdojade.comparator;

import org.osmdroid.util.GeoPoint;

import java.util.Comparator;

import miklukada.pl.takdojade.datamodel.Stop;

/**
 * Created by Barca on 2016-04-16.
 */
public class DistanceToUserComparator implements Comparator<Stop> {

    private final String TAG = getClass().getSimpleName();

    GeoPoint userPosition;

    public DistanceToUserComparator (GeoPoint userPosition){
        this.userPosition = userPosition;
    }

    @Override
    public int compare(Stop lhs, Stop rhs) {
        int distance1 = userPosition.distanceTo(new GeoPoint(lhs.getLat(), lhs.getLon()));
        int distance2 = userPosition.distanceTo(new GeoPoint(rhs.getLat(),rhs.getLon()));
        return distance1-distance2;
    }
}
