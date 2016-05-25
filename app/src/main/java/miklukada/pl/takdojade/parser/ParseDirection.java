package miklukada.pl.takdojade.parser;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Konrad on 2016-02-28.
 */
public class ParseDirection {

    private final String TAG = getClass().getSimpleName();

    private String path;

    public String parsePolylineFromDirections(String result){
        try {
            JSONObject directionJson = new JSONObject(result);
            JSONArray routes = directionJson.getJSONArray("routes");
            for (int i=0; i<routes.length(); i++){
                JSONObject object = routes.getJSONObject(i);
                JSONObject overview_polyline = object.getJSONObject("overview_polyline");
                JSONObject points = new JSONObject(overview_polyline.toString());
                path = points.getString("points");
                Log.v(TAG, "parsed direction response:" + path);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return path;
    }


}
