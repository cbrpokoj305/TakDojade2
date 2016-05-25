package miklukada.pl.takdojade.route;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import miklukada.pl.takdojade.R;
import miklukada.pl.takdojade.datamodel.Route;
import miklukada.pl.takdojade.datamodel.Stop;
import miklukada.pl.takdojade.markeroverlay.BusMarker;
import miklukada.pl.takdojade.markeroverlay.StopMarker;
import miklukada.pl.takdojade.utils.RouteStopsScheduleHelper;
import miklukada.pl.takdojade.wktdata.WKTLinestring;

/**
 * Created by Barca on 2016-04-17.
 */
public class BusDrawer {

    private final String TAG = getClass().getSimpleName();

    private final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs

    private Context context;
    private MapView mapView;
    private Route route;
    private String choosenStop;
    private int choosenStopIndex;
    private Handler mHandler;
    private Stop previousStop = null;
    private FlaotPoint[] flaotPoints = null;
    private ArrayList<GeoPoint> geoPointArrayList = new ArrayList<>();
    private BusMarker busMarker;
    private int index = 0;
    private boolean draw = false;
    private int count = 0;
    private int markerColor;
    private Bitmap bitmap;
    private GeoPoint geoPoint;
    private ArrayList<Schedule> schedules = new ArrayList<>();

    private final int TIME_SPENT_ON_STOP_IN_MILI_SEC = 15000;
    private final int FASTEST_INTERVAL_SEC = 30;
    private int REFRESH_IN_SEC = 2;

    private final int SEC = 30;

    private Stop lastStop = null;

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public BusMarker getBusMarker() {
        return busMarker;
    }

    public void setBusMarker(BusMarker busMarker) {
        this.busMarker = busMarker;
    }

    private boolean handlerStopped = false;

    public BusDrawer(final Context context, final MapView mapView, final String choosenStop, final Route route) {
        this.context = context;
        this.mapView = mapView;
        this.route = route;
        this.choosenStop = choosenStop;
        Log.v(TAG, "choosen stop:" + choosenStop);
        for (int i = 0; i < route.getStopsList().size(); i++) {
            if (route.getStopsList().get(i).getStopName().equals(choosenStop)) {
                choosenStopIndex = i;
            }
        }
        busMarker = new BusMarker(mapView, new GeoPoint(0, 0), route.getRouteId());
        busMarker.setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.button_blue2_128));

        prepareSchedule();

        mHandler = new Handler(Looper.getMainLooper()) {

            private boolean refreshBusPosition = true;
            private long differenceBetweenCurrentTimeAndFoundStopTime = 0;
            private long differenceBeetwenStops = 0;
            private int index = 0;

            @Override
            public void handleMessage(Message msg) {

                super.handleMessage(msg);
//                Log.v(TAG, "handler is working:" + msg.arg1);
                if (draw) {
                    msg.arg1++;
                    count = msg.arg1;
                    drawOnRoute(msg.arg1);

                    if (lastStop == null) {
                        lastStop = findStopWhereBusIs();
                        if (lastStop != null) {
                            Stop nextStop = getNextStop(lastStop);
                            Date nextStopTimeArrival = getTimeOfArrival(nextStop);
                            Date foundStopTimeArrival = getTimeOfArrival(lastStop);
                            Date currentTime = Calendar.getInstance().getTime();
                            String routeSeq = findRouteSeq(lastStop);
                            Log.v(TAG, "bus:"+route.getRouteId()+" route seq:" + routeSeq);
                            Log.v(TAG, "bus:"+route.getRouteId()+" lastStop:" + lastStop.getStopName());
                            if (foundStopTimeArrival != null && nextStopTimeArrival != null) {
                                differenceBeetwenStops = nextStopTimeArrival.getTime() - foundStopTimeArrival.getTime();
                                differenceBetweenCurrentTimeAndFoundStopTime = currentTime.getTime() - foundStopTimeArrival.getTime();
                                Log.v(TAG, "bus:"+route.getRouteId()+" time of arrival to:" + lastStop.getStopName() + " is:" + foundStopTimeArrival.toString());
                            }
                            if (nextStop != null) {
                                Log.v(TAG, "bus:"+route.getRouteId()+" next stop:" + nextStop.getStopName());
                                if (nextStopTimeArrival != null) {
                                    Log.v(TAG, "bus:"+route.getRouteId()+" time of arrival to:" + nextStop.getStopName() + " is:" + nextStopTimeArrival.toString());
                                }
                            }

                            Log.v(TAG, "bus:"+route.getRouteId()+" difference beetwen this and next stop:" + differenceBeetwenStops);
                            differenceBeetwenStops -= TIME_SPENT_ON_STOP_IN_MILI_SEC;
                            Log.v(TAG, "bus:"+route.getRouteId()+" difference beetwen current time and found stop time:" + differenceBetweenCurrentTimeAndFoundStopTime);
                            calculatePointsOnRouteSeq(routeSeq,differenceBeetwenStops/1000);
                            if (differenceBetweenCurrentTimeAndFoundStopTime < TIME_SPENT_ON_STOP_IN_MILI_SEC) {
                                busMarker.setPosition(new GeoPoint(lastStop.getLat(), lastStop.getLon()));
                                Log.v(TAG, "bus"+route.getRouteId()+" should be at stop");
                            } else {
                                double value = (double) differenceBetweenCurrentTimeAndFoundStopTime / (double) differenceBeetwenStops;
                                Log.v(TAG, "bus"+route.getRouteId()+" should be at " + (value * 100) + "% of seq");
                                if (value > 1){
                                    value = 1;
                                }
                                int size = geoPointArrayList.size()/2;
                                index = (int) (size * value);
                                busMarker.setPosition(geoPointArrayList.get(index));
                            }

                        } else
                            busMarker.setPosition(new GeoPoint(route.getStopsList().get(0).getLat(), route.getStopsList().get(0).getLon()));
                    } else {
                        Log.v(TAG, "for bus:" + route.getRouteId() + " index:" + index + " time difference:" + differenceBeetwenStops / 1000);
                        if (index == differenceBeetwenStops / 1000) {
                            refreshBusPosition = false;
                            Log.v(TAG, "bus:" + route.getRouteId() + " last stop:" + lastStop.getStopName());
                            final Date foundStopTimeArrival = getTimeOfArrival(lastStop);
                            final Stop nextStop = getNextStop(lastStop);
                            if (nextStop != null) {
                                final Date nextStopTimeArrival = getTimeOfArrival(nextStop);
                                Log.v(TAG, "bus:" + route.getRouteId() + " next stop:" + nextStop.getStopName());
                                busMarker.setPosition(new GeoPoint(nextStop.getLat(), nextStop.getLon()));
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshBusPosition = true;
                                        Log.v(TAG, "now bus:" + route.getRouteId() + " is at stop:" + nextStop.getStopName());
                                        String routeSeq = findRouteSeq(nextStop);
                                        differenceBeetwenStops = nextStopTimeArrival.getTime() - foundStopTimeArrival.getTime();
                                        differenceBeetwenStops -= TIME_SPENT_ON_STOP_IN_MILI_SEC;
                                        calculatePointsOnRouteSeq(routeSeq, differenceBeetwenStops/1000);
                                        Log.v(TAG, "bus:" + route.getRouteId() + " starts from:" + nextStop.getStopName() + " difference:" + differenceBeetwenStops);
                                        index = 0;
                                        lastStop = nextStop;
                                    }
                                }, TIME_SPENT_ON_STOP_IN_MILI_SEC);
                            }
                        }


                        if (geoPointArrayList.size() / 2 > index && refreshBusPosition) {
                            Log.v(TAG, "set position for bus:" + route.getRouteId() + " index:" + index + " geopoint:" + geoPointArrayList.get(index).toString());
                            busMarker.setPosition(geoPointArrayList.get(index));
                        }
                        index +=REFRESH_IN_SEC;

                    }

                }

            }
        };
    }

    public int getREFRESH_IN_SEC() {
        return REFRESH_IN_SEC;
    }

    public void setREFRESH_IN_SEC(int REFRESH_IN_SEC) {
        this.REFRESH_IN_SEC = REFRESH_IN_SEC;
    }

    private Stop getNextStop(Stop foundStop) {
        int index = 0;
        Stop nextStop = null;
        for (int i = 0; i < route.getStopsList().size(); i++) {
            if (route.getStopsList().get(i).getStopName().equals(foundStop.getStopName())) {
                index = i;
            }
        }
        if (index + 1 < route.getStopsList().size()) {
            nextStop = route.getStopsList().get(index + 1);
        }
        return nextStop;
    }

    private Date getTimeOfArrival(Stop stop) {
        if (stop != null) {
            for (int j = 0; j < schedules.size(); j++) {
                if (schedules.get(j).getStop().getStopName().equals(stop.getStopName())) {
                    return schedules.get(j).getTime();
                }
            }
        }
        return null;
    }


    private void calculatePointsOnRouteSeq(String routeSeqString, long interval) {
        geoPointArrayList.clear();
        for (int k = 0; k < route.getCalculatedPoints().size(); k++) {
            WKTReader wktReader = new WKTReader();
            WKTLinestring wktLinestring = route.getCalculatedPoints().get(k);
            String route_seq = wktLinestring.getName().replace("stop_sequence:", "");
            if (routeSeqString.equals(route_seq)) {
                try {
                    Geometry geometry = wktReader.read(wktLinestring.getLINESTRING());
                    for (int lineIndex = 0; lineIndex < geometry.getNumGeometries(); lineIndex++) {
                        Geometry lineGeometry = geometry.getGeometryN(lineIndex);
                        Coordinate[] lineCoordinates = lineGeometry.getCoordinates();
                        ArrayList<GeoPoint> geoPointsTemp = new ArrayList<>();

                        for (int index = 0; index < lineCoordinates.length; index++) {
                            GeoPoint gPt = new GeoPoint(lineCoordinates[index].y, lineCoordinates[index].x);
                            geoPointsTemp.add(gPt);
                        }
                        Path path = getPath(geoPointsTemp, mapView);

                        Log.v(TAG, "bus"+route.getRouteId()+" calculated interval:"+interval);
                        flaotPoints = getPoints(path, 2 * (int)interval);
                        for (int i = 0; i < flaotPoints.length; i++) {
//                                            Log.v(TAG, "founded points: x"+flaotPoints[i].getX()+" y:"+flaotPoints[i].getY());
                            IGeoPoint geoPoint = mapView.getProjection().fromPixels((int) flaotPoints[i].getX(), (int) flaotPoints[i].getY());
//                                                Log.v(TAG, "geopoint lat:" + geoPoint.getLatitude() + " lon:" + geoPoint.getLongitude());
                            geoPointArrayList.add(new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude()));
                        }


                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private String findRouteSeq(Stop correctStop) {
        String stopSeq = correctStop.getSequence().trim();
        int seq = Integer.parseInt(stopSeq);
        int routeSeq = seq + 1;
        String routeSeqString = "" + routeSeq;
        return routeSeqString;
    }


    public void scaleMarker(double width, double height) {
        busMarker.scaleMarker(width, height);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        busMarker.setBitmap(bitmap);
    }

    public void invlaidate() {
        mapView.getOverlays().remove(busMarker);
        int count = 0;
        for (int i = 0; i < mapView.getOverlays().size(); i++) {
            if (mapView.getOverlays().get(i) instanceof StopMarker)
                count++;
        }
        mapView.getOverlays().add(count, busMarker);
        mapView.invalidate();
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public void setMarkerColor(int markerColor) {
        this.markerColor = markerColor;
        busMarker.setPaintColor(markerColor);
        mapView.invalidate();
    }

    public boolean isDraw() {
        return draw;
    }

    public void setDraw(boolean draw) {
        this.draw = draw;
        mapView.getOverlays().add(busMarker);
        drawOnRoute(count);
    }

    public ArrayList<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(ArrayList<Schedule> schedules) {
        this.schedules = schedules;
    }

    @NonNull
    private Path getPath(ArrayList<GeoPoint> geoPointsTemp, MapView mapView) {
        Path path = new Path();
        for (int i = 0; i < geoPointsTemp.size(); i++) {
            Point point = new Point();
            Point fromProjection = mapView.getProjection().toPixels(geoPointsTemp.get(i), point);

            if (i == 0) {
                path.moveTo(fromProjection.x, fromProjection.y);
            }
            path.lineTo(fromProjection.x, fromProjection.y);
        }

        path.close();
        return path;
    }

    private void drawOnRoute(int i) {
        //Log.v(TAG, "put value:" + i + " to message");
        Message msg = new Message();
        msg.arg1 = i;
        if (!handlerStopped)
            mHandler.sendMessageDelayed(msg, REFRESH_IN_SEC * 1000);
    }

    public void stopHandler() {
        handlerStopped = true;
    }

    private void prepareSchedule() {
        String time = route.getTimeOfArrival().substring(route.getTimeOfArrival().indexOf("T") + 1, route.getTimeOfArrival().length() - 3);
        String[] timeHelper = time.split(":");
        int hour = Integer.parseInt(timeHelper[0]);
        int min = Integer.parseInt(timeHelper[1]);
        Date refDate = new Date();
        refDate.setHours(hour);
        refDate.setMinutes(min);
        ArrayList<RouteStopsScheduleHelper> routeStopsScheduleHelpers = new ArrayList<>();
        if (route.getIntervals().size() > 0) {
            for (int i1 = 0; i1 < route.getStopsList().size(); i1++) {
                if (i1 < choosenStopIndex) {
                    int period = 0;
                    int hourBefore = hour;
                    for (int j1 = i1; j1 <= choosenStopIndex - 1; j1++) {
                        period += Integer.parseInt(route.getIntervals().get(j1));
                    }
                    long t = refDate.getTime();
                    Date beforeDate = new Date(t - (period * ONE_MINUTE_IN_MILLIS));
                    Schedule schedule = new Schedule(beforeDate, route.getStopsList().get(i1));
                    schedules.add(schedule);
//                    Log.v(TAG, "bus:" + route.getRouteId() + " should be on stop:" + route.getStopsList().get(i1).getStopName() + " " + period + " min earlier" + " at:" + beforeDate.toLocaleString());
                    routeStopsScheduleHelpers.add(new RouteStopsScheduleHelper(beforeDate, route.getStopsList().get(i1)));
                } else if (i1 == choosenStopIndex) {
                    routeStopsScheduleHelpers.add(new RouteStopsScheduleHelper(refDate, route.getStopsList().get(i1)));
                    Schedule schedule = new Schedule(refDate, route.getStopsList().get(i1));
                    schedules.add(schedule);
//                    Log.v(TAG, "bus:" + route.getRouteId() + " should be on stop:" + route.getStopsList().get(i1).getStopName() + " at" + time);
                } else if (i1 > choosenStopIndex) {
                    int period = 0;
                    for (int j1 = choosenStopIndex; j1 < i1; j1++) {
                        period += Integer.parseInt(route.getIntervals().get(j1));
                    }
                    long t = refDate.getTime();
                    Date afterDate = new Date(t + (period * ONE_MINUTE_IN_MILLIS));
                    Schedule schedule = new Schedule(afterDate, route.getStopsList().get(i1));
                    schedules.add(schedule);
                    routeStopsScheduleHelpers.add(new RouteStopsScheduleHelper(afterDate, route.getStopsList().get(i1)));
//                    Log.v(TAG, "bus:" + route.getRouteId() + " should be on stop:" + route.getStopsList().get(i1).getStopName() + " " + period + " min later" + " at:" + afterDate.toLocaleString());
                }
            }
            route.setRouteStopsScheduleHelpers(routeStopsScheduleHelpers);
        }

        for (int i=0; i<schedules.size(); i++){
            Date date = schedules.get(i).getTime();
            date.setSeconds(SEC);
            schedules.get(i).setTime(date);
        }

        for (int i=0; i<schedules.size()-1; i++){
            Log.v(TAG, "schedules bus:" + route.getRouteId() + " stop:" + schedules.get(i).getStop().getStopName() + " time:" + schedules.get(i).getTime().toString());
            if (schedules.get(i).getTime().getTime() == schedules.get(i+1).getTime().getTime()){
                Log.v(TAG, "schedules for stop:"+schedules.get(i).getStop().getStopName()+" and stop:"+schedules.get(i+1).getStop().getStopName()+" are the same");
                Date date1 = schedules.get(i).getTime();
                date1.setSeconds(SEC-FASTEST_INTERVAL_SEC);
                schedules.get(i).setTime(date1);
                Date date2 =schedules.get(i+1).getTime();
                date2.setSeconds(SEC+FASTEST_INTERVAL_SEC);
                schedules.get(i+1).setTime(date2);
            }
        }

        for (int i=0; i<schedules.size()-1; i++){
            Log.v(TAG, "schedules after change bus:"+ route.getRouteId()+" stop:"+schedules.get(i).getStop().getStopName()+" time:"+schedules.get(i).getTime().toString());
        }

//        Log.v(TAG, "bus:" + route.getRouteId() + " stops on route:" + route.getStopsList().size() + " intervals number:" + route.getIntervals().size() + " choose stop index:" + choosenStopIndex);
    }

    public Stop findStopWhereBusIs() {

        ArrayList<RouteStopsScheduleHelper> routeStopsScheduleHelpers = route.getRouteStopsScheduleHelpers();
        Calendar calendar = Calendar.getInstance();
        Stop currentStop = null;
        for (int i = 0; i < routeStopsScheduleHelpers.size(); i++) {
            long timeDifference = calendar.getTime().getTime() - routeStopsScheduleHelpers.get(i).getDate().getTime();
//                Log.v(TAG, "for stop:"+routeStopsScheduleHelpers.get(i).getStop().getStopName()+" time difference:"+timeDifference);
            if (timeDifference < 0) {
                break;
            }
            currentStop = routeStopsScheduleHelpers.get(i).getStop();
        }
        return currentStop;
    }

    private FlaotPoint[] getPoints(Path path0, int count) {
        FlaotPoint[] pointArray = new FlaotPoint[count];
        PathMeasure pm = new PathMeasure(path0, false);
        float length = pm.getLength();
        float distance = 0f;
        float speed = length / count;
        int counter = 0;
        float[] aCoordinates = new float[2];

        while ((distance < length) && (counter < count)) {
            // get point from the path
            pm.getPosTan(distance, aCoordinates, null);
            pointArray[counter] = new FlaotPoint(aCoordinates[0],
                    aCoordinates[1]);
            counter++;
            distance = distance + speed;
            IGeoPoint geoPoint = mapView.getProjection().fromPixels((int) aCoordinates[0], (int) aCoordinates[1]);
//            Log.v(TAG, "geopoint lat:" + geoPoint.getLatitude() + " lon:" + geoPoint.getLongitude());
        }

        return pointArray;
    }

    class FlaotPoint {
        float x, y;

        public FlaotPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }


}
