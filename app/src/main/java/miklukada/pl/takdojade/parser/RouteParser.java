package miklukada.pl.takdojade.parser;

import android.content.Context;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import miklukada.pl.takdojade.datamodel.Route;
import miklukada.pl.takdojade.datamodel.Stop;
import miklukada.pl.takdojade.datamodel.Street;
import miklukada.pl.takdojade.wktdata.WKTLinestring;

/**
 * Created by Konrad on 2016-02-26.
 */
public class RouteParser {

    private final String TAG = getClass().getSimpleName();

    private Context context;

    private Geocoder geocoder;

    public RouteParser(Context context){
        this.context = context;
        geocoder = new Geocoder(context);
    }

    private ArrayList<Route> routesList = new ArrayList<>();

    public ArrayList<Route> parseRoutes (String result){
        try {
            JSONArray routesArray = new JSONArray(result);
            for (int i=0; i<routesArray.length(); i++){
                JSONObject route = routesArray.getJSONObject(i);
                String routeId = route.getString("route_id");
                String tripId = route.getString("trip_id");
                //Route tempRoute = new Route();
                JSONArray streets = route.getJSONArray("streets");
                ArrayList<Street> tempStreets = new ArrayList<>();

                int routType = route.getInt("route_type");

                for (int j=0; j<streets.length(); j++){
                    JSONObject street = streets.getJSONObject(j);
                    String cityName = street.getString("city_name");
                    String streetName = street.getString("street_name");
                    String sequence = street.getString("sequence");
                    Street finalStreet = new Street(cityName,streetName,sequence);
                    tempStreets.add(finalStreet);
                }
                JSONArray intervals = route.getJSONArray("stop_intervals");
                ArrayList<String> stopIntervals = new ArrayList<>();
                for (int index=0; index<intervals.length(); index++){
                    stopIntervals.add(intervals.getString(index));
                }

                JSONArray stops = route.getJSONArray("stops");
                ArrayList<Stop> tempStops = new ArrayList<>();
                for (int k=0; k<stops.length(); k++){
                    JSONObject stop = stops.getJSONObject(k);
                    String stopName = stop.getString("stop_name");
                    String stopId = stop.getString("stop_id");
                    String stopSequence = stop.getString("stop_sequence");
                    String busStopCoord = stop.getString("cord");
                    double lat = 0;
                    double lon = 0;
                    String streetName = "";

                    if (!busStopCoord.equals("null")) {
//                        Log.v(TAG, "busStop coordinates:"+busStopCoord);
                        if (!busStopCoord.equals("None")) {
                            String coordinates = busStopCoord.substring(busStopCoord.indexOf("(") + 1, busStopCoord.indexOf(")"));
                            String longitude = coordinates.substring(0, coordinates.indexOf(" "));
                            String latitude = coordinates.substring(coordinates.indexOf(" ") + 1);
                            lat = Double.parseDouble(latitude);
                            lon = Double.parseDouble(longitude);
                            Location location = new Location("");
                            location.setLatitude(lat);
                            location.setLongitude(lon);
                        }
                        //streetName = Helper.getStreet(geocoder, location);
                    }

                    Stop finalStop = new Stop();
                    finalStop.setStopName(stopName);
                    finalStop.setStopId(stopId);
                    finalStop.setLat(lat);
                    finalStop.setLon(lon);
                    finalStop.setStreetName(streetName);
                    finalStop.setSequence(stopSequence);
                    tempStops.add(finalStop);

                }
                JSONArray points = route.getJSONArray("route");
                ArrayList<WKTLinestring> tempWKTLinestring = new ArrayList<>();
                for (int l=0; l<points.length(); l++){
                    JSONObject object = points.getJSONObject(l);
                    String stop_sequence = object.getString("stop_sequence");
                    String way = object.getString("way");
                    String[] help = way.split(";");
                    WKTLinestring line = new WKTLinestring(help[1],"stop_sequence:"+stop_sequence);
                    tempWKTLinestring.add(line);

                }

                JSONArray array = route.getJSONArray("arrival_times");
                String time = array.getString(0);
                ArrayList<String> times = new ArrayList<>();
                for (int j=0; j<array.length(); j++){
                    String foramtedTime = array.getString(j).substring(array.getString(j).indexOf("T") + 1, array.getString(j).length() - 3);
                    times.add(foramtedTime);
                    Log.v(TAG, "time from adam:"+array.getString(j)+" for bus:"+routeId);
                    Route tempRoute = new Route();
                    tempRoute.setTimeOfArrival(array.getString(j));
                    tempRoute.setRouteId(routeId);
                    tempRoute.setStreetsList(tempStreets);
                    tempRoute.setTripId(tripId);
                    tempRoute.setStopsList(tempStops);
                    tempRoute.setCalculatedPoints(tempWKTLinestring);
                    tempRoute.setIntervals(stopIntervals);
                    tempRoute.setRouteType(routType);
                    routesList.add(tempRoute);
                }

//                tempRoute.setTimeOfArrival(time);
//                tempRoute.setRouteId(routeId);
//                tempRoute.setStreetsList(tempStreets);
//                tempRoute.setTripId(tripId);
//                tempRoute.setStopsList(tempStops);
//                tempRoute.setCalculatedPoints(tempWKTLinestring);
//                tempRoute.setIntervals(stopIntervals);
//                tempRoute.setRouteType(routType);
//                tempRoute.setTimes(times);
//                routesList.add(tempRoute);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return routesList;
    }
}
