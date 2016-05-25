package miklukada.pl.takdojade.parser;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import miklukada.pl.takdojade.wktdata.WKTLinestring;


/**
 * Created by Konrad on 2016-03-03.
 */
public class CityStreetParser {

    private final String TAG = getClass().getSimpleName();
    private ArrayList<WKTLinestring> wktLinestringArrayList = new ArrayList<>();

    public ArrayList<WKTLinestring> parseCityStreet(String result){
        try {
            JSONArray streetArray = new JSONArray(result);
            for (int i=0; i<streetArray.length(); i++){
                JSONObject streetObject = streetArray.getJSONObject(i);
                String name = streetObject.getString("name");
                String way = streetObject.getString("way");
                String[] way_parts = way.split(";");
                String linestring = way_parts[1];
                Log.v(TAG, "parsed linestring:" + linestring);
                wktLinestringArrayList.add(new WKTLinestring(linestring, name));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wktLinestringArrayList;
    }
}
