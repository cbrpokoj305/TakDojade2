package miklukada.pl.takdojade.markeroverlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import miklukada.pl.takdojade.R;
import miklukada.pl.takdojade.datamodel.Stop;
import miklukada.pl.takdojade.ztmdata.ZtmRouteDownloader;

/**
 * Created by Barca on 2016-04-16.
 */
public class StopMarker extends Marker {

    private final String TAG = getClass().getSimpleName();
    private Stop stop;
    private Context context;

    private int width = 100;
    private int height = 100;

    private Drawable d;
    private Bitmap bitmap;
    private Bitmap scaledBitmap = null;

    private MapView mapView;
    private Drawable markerIcon;

    private TextView stopName;
    private int resId;
    private String type;

    private GeoPoint position;


    public StopMarker(MapView mapView, final Stop stop, final Context context, Drawable markerIcon) {
        super(mapView);
        this.stop = stop;
        this.context = context;
        this.mapView = mapView;
        this.resId = resId;
        this.markerIcon = markerIcon;
        this.position = new GeoPoint(stop.getLat(), stop.getLon());


        if (markerIcon != null) {
            bitmap = ((BitmapDrawable) markerIcon).getBitmap();
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            d = new BitmapDrawable(context.getResources(), scaledBitmap);
            setIcon(d);
        }
        setPosition(new GeoPoint(stop.getLat(), stop.getLon()));
        setTitle(stop.getStopName());
        setSnippet(stop.getStopId());

//        StopInfoWindowView stopInfoWindowView = new StopInfoWindowView(R.layout.info_window_layout,mapView, stop.getStopName());
//        setInfoWindow(stopInfoWindowView);


        System.gc();
    }

    public double getIconWidth(){
        if (d == null)
            return width;
        return d.getIntrinsicWidth();
    }

    public double getIconHeight(){
        if (d == null)
            return height;
        return d.getIntrinsicHeight();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TextView getStopName() {
        return stopName;
    }

    public void setStopName(TextView stopName) {
        this.stopName = stopName;
    }

    public Drawable getMarkerIcon() {
        return markerIcon;
    }

    public void setMarkerIcon(Drawable markerIcon) {
        this.markerIcon = markerIcon;
        if (markerIcon != null) {
            bitmap = ((BitmapDrawable) markerIcon).getBitmap();
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            d = new BitmapDrawable(context.getResources(), scaledBitmap);
            setIcon(d);
        }
    }

    public void scaleMarker(double width, double height){
        if (markerIcon != null) {
            d = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(scaledBitmap,(int)(width), (int)(height), true));
            setIcon(d);
        }
    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }



    private class StopInfoWindowView extends InfoWindow{

        private Button search;
        private TextView title;

        public StopInfoWindowView(int layoutResId, final MapView mapView, String name) {
            super(layoutResId, mapView);
            search = (Button)getView().findViewById(R.id.button);
            title = (TextView)getView().findViewById(R.id.routeId);
            title.setText(name);
            search.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "you click search button");
                    new ZtmRouteDownloader(context).execute(stop.getStopId());
                    if (stopName != null){
                        stopName.setText(stop.getStopName());
                    }
                    close();
                }
            });
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    close();
                }
            });
        }

        @Override
        public void onOpen(Object item) {
            for (int i=0; i<mapView.getOverlays().size(); i++){
                if (mapView.getOverlays().get(i) instanceof StopMarker){
                    ((StopMarker) mapView.getOverlays().get(i)).getInfoWindow().close();
                }
            }
        }



        @Override
        public void onClose() {

        }
    }


}
