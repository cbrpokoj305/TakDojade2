package miklukada.pl.takdojade.ztmdata;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import miklukada.pl.takdojade.utils.ConstantValues;
import miklukada.pl.takdojade.utils.StreamReader;


/**
 * Created by Konrad on 2016-03-03.
 */
public class CrossStreetDownloader extends AsyncTask<String, String, String> {


    private final String TAG = getClass().getSimpleName();

//    private final String url_prefix = "http://217.96.70.94:8000/rest_api/intersections?first=Wolska&second=P%C5%82ocka";
private final String url_prefix = "http://217.96.70.94:8000/rest_api/intersections?first=";

    private Context context;

    public CrossStreetDownloader(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        StringBuilder builder = new StringBuilder();
        String first_street = params[0];
        String second_street = params[1];
        Log.v(TAG, "street before coding:" + params[0]);
        try {
            first_street = URLEncoder.encode(params[0], "UTF-8");
            second_street = URLEncoder.encode(params[1], "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        builder.append(url_prefix);
        builder.append(first_street);
        builder.append("&second=");
        builder.append(second_street);
        String response = "";
        try {
            response = downloadUrl(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Log.v(TAG, "response form server:" + s);
        Intent intent = new Intent();
        intent.setAction(ConstantValues.CROSS_ACTION);
        intent.putExtra(ConstantValues.CROSS_DATA, s);
        context.sendBroadcast(intent);
        Log.v(TAG, "send broadcast data:" + s.length());
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
