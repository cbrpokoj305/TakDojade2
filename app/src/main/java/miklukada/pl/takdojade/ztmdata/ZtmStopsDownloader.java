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
 * Created by Konrad on 2016-02-26.
 */
public class ZtmStopsDownloader extends AsyncTask<Double, String, String> {

    private final String TAG = getClass().getSimpleName();

    private final String url = "http://217.96.70.94:8000/rest_api/near_stops?distance=90&lat=52.2379553&lon=20.9636243";

    private final String url_prefix = "http://217.96.70.94:8000/rest_api/near_stops?distance=";

    private final String url_all_stops = "http://217.96.70.94:8000/rest_api/stops/";

    private double distance, lat, lon;

    private Context context;

    public static boolean downloadInProgress = false;

    private String type;

    public ZtmStopsDownloader(Context context, String type) {
        this.context = context;
        this.type = type;
    }

    @Override
    protected String doInBackground(Double... params) {
        StringBuilder builder = new StringBuilder();
        if (params.length == 3) {
            distance = params[0];
            lat = params[1];
            lon = params[2];
            builder.append(url_prefix);
            builder.append(distance);
            builder.append("&lat=");
            builder.append(lat);
            builder.append("&lon=");
            builder.append(lon);
        }

        String finalUrl = builder.toString();
        String results = "";
        Log.v(TAG, "ztm stops downloader:"+finalUrl);
        try {
            results = downloadUrl(finalUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.v(TAG, "results from Adam:" + result);
        ZtmStopsDownloader.downloadInProgress = false;
        Intent intent = new Intent();
        intent.setAction(ConstantValues.NEAREST_STOPS_ACTION);
        intent.putExtra(ConstantValues.NEAREST_STOPS_DATA, result);
        intent.putExtra(ConstantValues.STOP_TYPE, type);
        context.sendBroadcast(intent);


    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        ZtmStopsDownloader.downloadInProgress = true;

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
