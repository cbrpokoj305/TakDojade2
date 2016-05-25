package miklukada.pl.takdojade.parser;

import android.content.Context;
import android.location.Geocoder;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import miklukada.pl.takdojade.datamodel.Stop;


/**
 * Created by Konrad on 2016-02-26.
 */
public class StopParser {

    private ArrayList<Stop> stopsList = new ArrayList<>();
    private Context context;
    private Geocoder geocoder;

    public StopParser(Context context){
        this.context = context;
        geocoder = new Geocoder(context);
    }

    public ArrayList<Stop> getStopsList(String result){
        try {
            JSONArray array = new JSONArray(result);
            for (int i=0; i<array.length(); i++){
                //Log.v(TAG, "readed value:"+array.get(i).toString());
                JSONObject object = new JSONObject(array.get(i).toString());
                String busStopId = object.getString("stop_id");
                String busStopName = object.getString("stop_name");
                String busStopCoord = object.getString("cord");
                JSONArray stopTypeJsonArray = object.getJSONArray("route_type");
                int [] arrayType = new int[stopTypeJsonArray.length()];
                for (int j=0; j<stopTypeJsonArray.length(); j++)
                    arrayType[j] = stopTypeJsonArray.getInt(j);
                if (!busStopCoord.equals("null")) {
                    String coordinates = busStopCoord.substring(busStopCoord.indexOf("(") + 1, busStopCoord.indexOf(")"));
                    String longitude = coordinates.substring(0, coordinates.indexOf(" "));
                    String latitude = coordinates.substring(coordinates.indexOf(" ")+1);
                    //Log.v(TAG, "latitude:" + latitude + " longitude:" + longitude);
                    Location location = new Location("");
                    location.setLatitude(Double.parseDouble(latitude));
                    location.setLongitude(Double.parseDouble(longitude));
                    //String streetName = Helper.getStreet(geocoder, location);
                    stopsList.add(createBusStop(busStopId, busStopName,latitude,longitude,"", arrayType));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stopsList;
    }

    private Stop createBusStop (String id, String name, String latitude, String longitude, String streetName, int[] type){
        Stop busStop = new Stop();
        busStop.setStopId(id);
        busStop.setStopName(name);
        busStop.setLat(Double.parseDouble(latitude));
        busStop.setLon(Double.parseDouble(longitude));
        busStop.setStreetName(streetName);
        busStop.setStopType(type);
        return busStop;
    }
}
