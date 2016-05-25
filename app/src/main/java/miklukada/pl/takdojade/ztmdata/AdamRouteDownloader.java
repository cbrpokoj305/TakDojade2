package miklukada.pl.takdojade.ztmdata;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import miklukada.pl.takdojade.utils.ConstantValues;
import miklukada.pl.takdojade.utils.StreamReader;


/**
 * Created by Konrad on 2016-03-11.
 */
public class AdamRouteDownloader extends AsyncTask<String, String, String> {

    private final String TAG = getClass().getSimpleName();

    private final String url_prefix = "http://217.96.70.94:8000/rest_api/route?first_lat=";

//    private final String url_prefix = "http://217.96.70.94:8000/rest_api/route?first_lat=52.231349&first_lon=20.967223&second_lat=52.231576&second_lon=20.972388";

    private Context context;

    private String first_lat, first_lon, second_lat, second_lon;


    public AdamRouteDownloader(Context context){
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Log.v(TAG, "response form server:" + s);
        Intent intent = new Intent();
        intent.setAction(ConstantValues.CROSS_ADAM_ROUTE_ACTION);
        intent.putExtra(ConstantValues.CROSS_ADAM_ROUTE_DATA, s);
        context.sendBroadcast(intent);
        Log.v(TAG, "send broadcast data:" + s.length());
    }

    @Override
    protected String doInBackground(String... params) {
        String result = "";
        StringBuilder builder = new StringBuilder();
        builder.append(url_prefix);
        first_lat = params[0];
        first_lon = params[1];
        second_lat = params[2];
        second_lon = params[3];
        builder.append(first_lat);
        builder.append("&first_lon=");
        builder.append(first_lon);
        builder.append("&second_lat=");
        builder.append(second_lat);
        builder.append("&second_lon=");
        builder.append(second_lon);
        if (params.length > 4 && params[4]!= null)
            builder.append(params[4]);
        try {
            result = downloadUrl(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        Log.v(TAG, "try to download url:" + myurl);

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            is = conn.getInputStream();

            return StreamReader.convertStreamToString(is);

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
