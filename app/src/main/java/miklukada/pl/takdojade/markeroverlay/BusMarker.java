package miklukada.pl.takdojade.markeroverlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

/**
 * Created by Barca on 2016-04-17.
 */
public class BusMarker extends Marker {

    private final String TAG = getClass().getSimpleName();

    private GeoPoint position;

    private String routeId;

    private int paintColor = Color.BLACK;

    private Bitmap bitmap;

    private int width = 128;

    private int height = 128;

    private int textColor;

    private int textSize = 28;

    private int textScaleSize = 28;

    private Paint paint;

    private Point point;


    public BusMarker(MapView mapView, GeoPoint position, String routeId) {

        super(mapView);

        this.position = position;

        this.routeId = routeId;

        try {
            if (Integer.parseInt(routeId) >= 500 && Integer.parseInt(routeId) < 600) {
                textColor = Color.RED;
            } else {
                textColor = Color.BLACK;
            }
        } catch (NumberFormatException ex) {

        }

        paint = new Paint();
        paint.setTextSize(textScaleSize);
        point = new Point();
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    public void scaleMarker(double width, double height){
        this.bitmap = Bitmap.createScaledBitmap(bitmap, (int)width, (int)height, true);
        double scaleFacotr = width/this.width;
        textScaleSize = (int)(textSize*scaleFacotr);
        paint.setTextSize(textScaleSize);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public int getPaintColor() {
        return paintColor;
    }

    public void setPaintColor(int paintColor) {
        this.paintColor = paintColor;
    }

    @Override
    public GeoPoint getPosition() {
        return position;
    }

    @Override
    public void setPosition(GeoPoint position) {
        this.position = position;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);

        canvas.drawBitmap(bitmap, mapView.getProjection().toPixels(position, point).x - bitmap.getWidth() / 2, mapView.getProjection().toPixels(position, point).y - bitmap.getHeight() / 2, paint);
//        canvas.drawCircle(mapView.getProjection().toPixels(position, point).x,mapView.getProjection().toPixels(position, point).y, 50, paint );
        canvas.drawText(routeId, mapView.getProjection().toPixels(position, point).x, mapView.getProjection().toPixels(position, point).y+5, paint);

    }
}
