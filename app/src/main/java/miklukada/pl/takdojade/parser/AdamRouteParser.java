package miklukada.pl.takdojade.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import miklukada.pl.takdojade.wktdata.WKTLinestring;


/**
 * Created by Konrad on 2016-03-11.
 */
public class AdamRouteParser {

    private final String TAG = getClass().getSimpleName();
    private ArrayList<WKTLinestring> wktLinestringArrayList = new ArrayList<>();

    public ArrayList<WKTLinestring> parseResponse(String result){
        try {
            JSONArray routeArray = new JSONArray(result);
            for (int i=0; i<routeArray.length(); i++){
                JSONObject simplePath = routeArray.getJSONObject(i);
                String distance = simplePath.getString("cost");
                String className = simplePath.getString("class_name");
                String sequence = simplePath.getString("sequence");
                String way = simplePath.getString("way");
                String[] help = way.split(";");
                WKTLinestring line = new WKTLinestring(help[1],"seq:"+sequence+" class:"+className+" cost:"+distance);
                wktLinestringArrayList.add(line);
//                Log.v(TAG, "sequence:"+sequence+" way:"+way);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wktLinestringArrayList;
    }
}
