package miklukada.pl.takdojade.parser;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import miklukada.pl.takdojade.wktdata.WKTCrossLineString;
import miklukada.pl.takdojade.wktdata.WKTLinestring;


/**
 * Created by Konrad on 2016-03-03.
 */
public class StreetCrossParser {

    private final String TAG = getClass().getSimpleName();

    private ArrayList<WKTCrossLineString> wktCrossLineStrings = new ArrayList<>();

    public ArrayList<WKTCrossLineString> parseCrossStreet(String result){
        try {
            JSONArray crossStreet = new JSONArray(result);
            for (int i=0; i<crossStreet.length(); i++){
                JSONObject object = crossStreet.getJSONObject(i);
                String firstName = object.getString("first_name");
                String secondName = object.getString("second_name");
                String intersection = object.getString("intersection");
                String secondWay = object.getString("second_way");
                String firstWay = object.getString("first_way");
                String[] first_way_parts = firstWay.split(";");
                String[] second_way_parts = secondWay.split(";");
                String[] intersection_parts = intersection.split(";");
                Log.v(TAG, "first way:" + first_way_parts[1]);
                Log.v(TAG, "second way:" + second_way_parts[1]);
                Log.v(TAG, "intersection:" + intersection_parts[1]);
                WKTLinestring first = new WKTLinestring(first_way_parts[1],firstName);
                WKTLinestring intersections = new WKTLinestring(intersection_parts[1],"intersection");
                WKTLinestring second = new WKTLinestring(second_way_parts[1],secondName);
                WKTCrossLineString wktCrossLineString = new WKTCrossLineString(intersections, first, second);
                wktCrossLineStrings.add(wktCrossLineString);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wktCrossLineStrings;
    }
}
