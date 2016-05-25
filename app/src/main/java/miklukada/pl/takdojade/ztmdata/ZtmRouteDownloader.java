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
public class ZtmRouteDownloader extends AsyncTask<String, String, String> {

    private final String TAG = getClass().getSimpleName();

    private final String url = "http://217.96.70.94:8000/rest_api/stops/502601/routes";

    private final String url_prefix = "http://217.96.70.94:8000/rest_api/stops/";

    private Context context;

    public ZtmRouteDownloader(Context context){
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.v(TAG, "results from Adam:" + result);
        Intent intent = new Intent();
        intent.setAction(ConstantValues.ROUTES_ACTION);
        intent.putExtra(ConstantValues.ROUTES_DATA, result);
        context.sendBroadcast(intent);
    }

    @Override
    protected String doInBackground(String... params) {
        String stopId = params[0];
        StringBuilder builder = new StringBuilder();
        builder.append(url_prefix);
        builder.append(stopId);
        builder.append("/routes");
        builder.append("?tplus=20&tminus=-2");
        String finalUrl = builder.toString();
        String results = "";
        Log.v(TAG, "ztm route downloader:"+finalUrl);
        try {
            results = downloadUrl(finalUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;


        try {
            URL url = new URL(myurl);
            Log.v(TAG, "url:" + myurl);
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
