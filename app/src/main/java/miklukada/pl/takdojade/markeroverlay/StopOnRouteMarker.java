package miklukada.pl.takdojade.markeroverlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import miklukada.pl.takdojade.datamodel.Stop;

/**
 * Created by Barca on 2016-04-16.
 */
public class StopOnRouteMarker extends Marker {

    private final String TAG = getClass().getSimpleName();
    private Stop stop;
    private Context context;

    private int width = 100;
    private int height = 100;

    private Drawable dr,d;
    private Bitmap bitmap;
    private Bitmap scaledBitmap = null;

    private MapView mapView;
    private Drawable markerIcon;

    private String routeId;


    public StopOnRouteMarker(MapView mapView, Stop stop, Context context, Drawable markerIcon) {
        super(mapView);
        this.stop = stop;
        this.context = context;
        this.mapView = mapView;
        this.markerIcon = markerIcon;

        if (markerIcon != null) {
            bitmap = ((BitmapDrawable) markerIcon).getBitmap();
            d = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, width, height, true));
            setIcon(d);
        }
        setPosition(new GeoPoint(stop.getLat(), stop.getLon()));
        setTitle(stop.getStopName());
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public Drawable getMarkerIcon() {
        return markerIcon;
    }

    public void setMarkerIcon(Drawable markerIcon) {
        this.markerIcon = markerIcon;

    }

    public void scaleMarker(double width, double height){
        if (markerIcon != null) {
            d = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, (int)(width), (int)(height), true));
            setIcon(d);

            mapView.invalidate();
        }
    }
}
