package miklukada.pl.takdojade.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import miklukada.pl.takdojade.R;
import miklukada.pl.takdojade.comparator.DistanceToUserComparator;
import miklukada.pl.takdojade.comparator.ScheduleComparator;
import miklukada.pl.takdojade.comparator.TimeComparatorForBuses;
import miklukada.pl.takdojade.datamodel.Route;
import miklukada.pl.takdojade.datamodel.Stop;
import miklukada.pl.takdojade.markeroverlay.StopMarker;
import miklukada.pl.takdojade.parser.RouteParser;
import miklukada.pl.takdojade.parser.StopParser;
import miklukada.pl.takdojade.route.BusDrawer;
import miklukada.pl.takdojade.utils.ConstantValues;
import miklukada.pl.takdojade.utils.CustomResourceProxy;
import miklukada.pl.takdojade.wktdata.WKTLinestring;
import miklukada.pl.takdojade.ztmdata.RouteWayCoordinatesHelper;
import miklukada.pl.takdojade.ztmdata.ZtmRouteDownloader;
import miklukada.pl.takdojade.ztmdata.ZtmStopsDownloader;

public class MapActivityTwo extends Activity implements MapEventsReceiver {

    private final String TAG = getClass().getSimpleName();
    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private final double DISTANCE_IN_METERS = 1000;

    public final static int STOP_TYPE_TRAM = 0;
    public final static int STOP_TYPE_METRO = 1;
    public final static int STOP_TYPE_SKM = 2;
    public final static int STOP_TYPE_BUS = 3;

    private TextView textViewNearestStop;
    private ImageView myLocation;
    private LinearLayout layout;


    private MapView mMapView;
    private MyLocationNewOverlay myLocationNewOverlay;
    private CustomResourceProxy mResourceProxy;
    private IMapController mapController;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private NearestStopReceiver nearestStopReceiver;
    private RoutesReceiver routesReceiver;
    private ArrayList<Stop> stops = new ArrayList<>();
    private ArrayList<Stop> changeStops = new ArrayList<>();
    private boolean canDownload = false;
    private GeoPoint userPosition = null;
    private boolean select = false;

    private ArrayList<Route> routes = new ArrayList<>();
    private ArrayList<ArrayList<Stop>> changesStops = new ArrayList<>();
    private ArrayList<BusDrawer> buses = new ArrayList<>();
    private ArrayList<BusDrawer> busesAddedToMap = new ArrayList<>();
    private ArrayList<ArrayList<Route>> downloadedRoutes = new ArrayList<>();
    private ArrayList<ArrayList<Polyline>> routeCoordinates = new ArrayList<>();
    private ArrayList<ArrayList<RouteWayCoordinatesHelper>> routeWayCoordinatesList = new ArrayList<>();

    private ArrayList<StopMarker> changesStopList = new ArrayList<>();
    private ArrayList<StopMarker> allStopList = new ArrayList<>();
    private ArrayList<StopMarker> onRouteStopList = new ArrayList<>();


    private ArrayList<Integer> routesChoosenByUser = new ArrayList<>();
    final ArrayList<Integer> checkboxIdList = new ArrayList<>();
    final ArrayList<CheckBox> checkboxList = new ArrayList<>();
    private Drawable busBlueIcon, busBlue2Icon, busGrayIcon;
    private Drawable tramBlueIcon, tramBlue2Icon, tramGrayIcon;
    private Drawable skmBlueIcon, skmBlue2Icon, skmGrayIcon;
    private Drawable metroBlueIcon, metroBlue2Icon, metroGrayIcon;


    private ArrayList<StopMarker> allStopsMarkers = new ArrayList<>();

    private Random random = new Random();

    private long checkScrollTime = 0;

    private TextView debug;

    private String BLUE = "#0B04B9";

    private String LIGHT_BLUE = "#BF5B79D5";

    private final int CHANGE_STOP_DISTANCE = 200;

    private double MAIN_STOP_SCALE_FACTOR = 1.25;
    private double ON_ROUTE_STOP_SCALE_FACTOR = 1;
    private double CHANGE_STOP_FACTOR = 0.75;
    private double NORMAL_STOP_SCALE_FACTOR = 0.75;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }


        debug = (TextView) findViewById(R.id.debug);
        textViewNearestStop = (TextView) findViewById(R.id.stopName);
        myLocation = (ImageView) findViewById(R.id.myLocation);
        layout = (LinearLayout) findViewById(R.id.mainLayout);

        registerBroadcasts();
        initializeMap();
        initializeMarkersIcon();
    }


    private void initializeMarkersIcon() {
        busBlueIcon = getResources().getDrawable(R.drawable.pin_blue_bus_128);
        busBlue2Icon = getResources().getDrawable(R.drawable.pin_blue2_bus_128);
        busGrayIcon = getResources().getDrawable(R.drawable.pin_gray_bus_128);

        tramBlueIcon = getResources().getDrawable(R.drawable.pin_blue_tram_128);
        tramBlue2Icon = getResources().getDrawable(R.drawable.pin_blue2_tram_128);
        tramGrayIcon = getResources().getDrawable(R.drawable.pin_gray_tram_128);

        skmBlueIcon = getResources().getDrawable(R.drawable.pin_blue_skm_128);
        skmBlue2Icon = getResources().getDrawable(R.drawable.pin_blue2_skm_128);
        skmGrayIcon = getResources().getDrawable(R.drawable.pin_gray_skm_128);

        metroBlueIcon = getResources().getDrawable(R.drawable.pin_blue_metro_128);
        metroBlue2Icon = getResources().getDrawable(R.drawable.pin_blue2_metro_128);
        metroGrayIcon = getResources().getDrawable(R.drawable.pin_gray_metro_128);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcasts();
        for (int i = 0; i < busesAddedToMap.size(); i++) {
            busesAddedToMap.get(i).stopHandler();
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    private void initializeMap() {

        final DisplayMetrics dm = getResources().getDisplayMetrics();

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.setMaxZoomLevel(18);

        mMapView.setTileSource(TileSourceFactory.MAPNIK);

        mResourceProxy = new CustomResourceProxy(this);

        GpsMyLocationProvider gpsLocationProvider = new GpsMyLocationProvider(this);
        gpsLocationProvider.setLocationUpdateMinTime(1000);
        gpsLocationProvider.setLocationUpdateMinDistance(10);
        myLocationNewOverlay = new MyLocationNewOverlay(this, mMapView);
        myLocationNewOverlay.enableMyLocation(gpsLocationProvider);
        myLocationNewOverlay.enableMyLocation();
        myLocationNewOverlay.enableFollowLocation();
        myLocationNewOverlay.setOptionsMenuEnabled(true);


        mapController = mMapView.getController();
        mapController.setZoom(mMapView.getMaxZoomLevel());

        GeoPoint defaultPosition = new GeoPoint(52.176524, 21.001212);
        userPosition = defaultPosition;
        mapController.setCenter(defaultPosition);
        mMapView.setMinZoomLevel(12);
        mapController.setZoom(16);

        this.mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this),
                mMapView);
        mCompassOverlay.enableCompass();

        mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);

        mRotationGestureOverlay = new RotationGestureOverlay(this, mMapView);
        mRotationGestureOverlay.setEnabled(true);

        mMapView.setTilesScaledToDpi(true);
        //mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        mMapView.setFlingEnabled(true);
        mMapView.setClickable(true);
        mMapView.setLongClickable(true);
        MapEventsOverlay evOverlay = new MapEventsOverlay(this, this);
        mMapView.getOverlays().add(evOverlay);

        mMapView.getOverlays().add(myLocationNewOverlay);
        //mMapView.getOverlays().add(this.mCompassOverlay);
        mMapView.getOverlays().add(this.mScaleBarOverlay);

        mMapView.setMapListener(new MapListener() {

            private boolean scrollIsWorking = false;

            long currentScrollTime;

            @Override
            public boolean onScroll(ScrollEvent event) {
                currentScrollTime = Calendar.getInstance().getTimeInMillis();

                if (!scrollIsWorking) {
                    scrollIsWorking = true;
                    Timer timer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (Calendar.getInstance().getTimeInMillis() - currentScrollTime > 200) {
                                Log.v(TAG, "scroll stops");
                                searchChangeStops();
                                scrollIsWorking = false;
                                this.cancel();
                                if (mMapView.getZoomLevel() > 14) {
                                    int count = 0;
                                    for (int i = 0; i < mMapView.getOverlays().size(); i++) {
                                        if (mMapView.getOverlays().get(i) instanceof StopMarker) {
                                            if (mMapView.getBoundingBox().contains(((StopMarker) mMapView.getOverlays().get(i)).getPosition())) {
                                                count++;
                                            }
                                        }
                                    }
                                    Log.v(TAG, "scroll - marker count:" + count);
                                    if (count < 5) {
                                        if (!ZtmStopsDownloader.downloadInProgress) {
                                            Log.v(TAG, "brak markerów na mapie, dociągnij nowe");
                                            if (userPosition == null)
                                                userPosition = new GeoPoint(mMapView.getMapCenter().getLatitude(), mMapView.getMapCenter().getLongitude());
                                            new ZtmStopsDownloader(MapActivityTwo.this, ConstantValues.STOP_NORMAL).execute(DISTANCE_IN_METERS, mMapView.getMapCenter().getLatitude(), mMapView.getMapCenter().getLongitude());
                                        }
                                    }
                                }
                            }
                        }
                    };
                    timer.scheduleAtFixedRate(timerTask, 0, 200);
                }


                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                Log.v(TAG, "current zoom level:" + event.getZoomLevel());
                scaleMarkers(event.getZoomLevel());
                scaleBusMarker(event.getZoomLevel());
                return false;
            }
        });

        myLocationNewOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
//                GeoPoint geoPoint = new GeoPoint(52.176524, 21.001212);
                userPosition = myLocationNewOverlay.getMyLocation();
                mapController.animateTo(userPosition);
                new ZtmStopsDownloader(MapActivityTwo.this, ConstantValues.STOP_NORMAL).execute(DISTANCE_IN_METERS, userPosition.getLatitude(), userPosition.getLongitude());
                select = true;
            }
        });

        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userPosition != null) {
                    mMapView.getController().animateTo(userPosition);
                    mapController.zoomTo(15);
                }
            }
        });


    }

    private void scaleMarkers(int zoom) {
        switch (zoom) {
            case 10:
                scaleMarkers(30, 30, zoom);
                break;
            case 11:
                scaleMarkers(40, 40, zoom);
                break;
            case 12:
                scaleMarkers(50, 50, zoom);
                break;
            case 13:
                scaleMarkers(70, 70, zoom);
                break;
            case 14:
                scaleMarkers(80, 80, zoom);
                break;
            case 15:
                scaleMarkers(90, 90, zoom);
                break;
            case 16:
                scaleMarkers(100, 100, zoom);
                break;
            case 17:
                scaleMarkers(110, 110, zoom);
                break;
            case 18:
                scaleMarkers(120, 120, zoom);
                break;
            case 19:
                scaleMarkers(120, 120, zoom);
                break;
        }
    }

    private void scaleMarkers(final int width, final int height, final int zoom) {
        for (int i = 0; i < mMapView.getOverlays().size(); i++) {
            if (mMapView.getOverlays().get(i) instanceof StopMarker) {
                if (((StopMarker) mMapView.getOverlays().get(i)).getType().equals(ConstantValues.STOP_ON_ROUTE)) {
                    if (((StopMarker) mMapView.getOverlays().get(i)).getIconWidth() != width && ((StopMarker) mMapView.getOverlays().get(i)).getIconHeight() != height)
                        ((StopMarker) mMapView.getOverlays().get(i)).scaleMarker(width, height);
                } else if (((StopMarker) mMapView.getOverlays().get(i)).getType().equals(ConstantValues.STOP_CHANGE)) {
                    if (((StopMarker) mMapView.getOverlays().get(i)).getIconWidth() != CHANGE_STOP_FACTOR * width && ((StopMarker) mMapView.getOverlays().get(i)).getIconHeight() != CHANGE_STOP_FACTOR * height)
                        ((StopMarker) mMapView.getOverlays().get(i)).scaleMarker(CHANGE_STOP_FACTOR * width, CHANGE_STOP_FACTOR * height);
                } else if (((StopMarker) mMapView.getOverlays().get(i)).getType().equals(ConstantValues.STOP_NORMAL)) {
                    if (((StopMarker) mMapView.getOverlays().get(i)).getIconWidth() != NORMAL_STOP_SCALE_FACTOR * width && ((StopMarker) mMapView.getOverlays().get(i)).getIconHeight() != NORMAL_STOP_SCALE_FACTOR * height)
                        ((StopMarker) mMapView.getOverlays().get(i)).scaleMarker(NORMAL_STOP_SCALE_FACTOR * width, NORMAL_STOP_SCALE_FACTOR * height);
                } else if (((StopMarker) mMapView.getOverlays().get(i)).getType().equals(ConstantValues.STOP_MAIN)) {
//                    Log.v(TAG, "change scale of main stop");
                    if (((StopMarker) mMapView.getOverlays().get(i)).getIconWidth() != MAIN_STOP_SCALE_FACTOR * width && ((StopMarker) mMapView.getOverlays().get(i)).getIconHeight() != MAIN_STOP_SCALE_FACTOR * height)
                        ((StopMarker) mMapView.getOverlays().get(i)).scaleMarker(MAIN_STOP_SCALE_FACTOR * width, MAIN_STOP_SCALE_FACTOR * height);
                }
            }
        }

       /* runOnUiThread(new Runnable() {
            @Override
            public void run() {
                debug.setText("zoom:" + zoom + " marker w:" + width + " h:" + height);
                mMapView.invalidate();
            }
        });*/

    }

    private void scaleBusMarker(final int zoomLevel) {
        double width = 100;
        double height = 100;
        switch (zoomLevel) {
            case 12:
                width = 50;
                height = 50;
                break;
            case 13:
                width = 70;
                height = 70;
                break;
            case 14:
                width = 80;
                height = 80;
                break;
            case 15:
                width = 90;
                height = 90;
                break;
            case 16:
                width = 100;
                height = 100;
                break;
            case 17:
                width = 110;
                height = 110;
                break;
            case 18:
                width = 120;
                height = 120;
                break;
        }
        for (int i = 0; i < busesAddedToMap.size(); i++) {
            busesAddedToMap.get(i).scaleMarker(width, height);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMapView.invalidate();
            }
        });
    }


    private class NearestStopReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "NearestStopReceiver received data:" + intent.getStringExtra(ConstantValues.NEAREST_STOPS_DATA) + " length:" + intent.getStringExtra(ConstantValues.NEAREST_STOPS_DATA).length());
            if (intent.getStringExtra(ConstantValues.NEAREST_STOPS_DATA).length() > 3) {
                String type = intent.getStringExtra(ConstantValues.STOP_TYPE);
                StopParser stopParser = new StopParser(MapActivityTwo.this);
                if (type.equals(ConstantValues.STOP_NORMAL)) {
                    stops = stopParser.getStopsList(intent.getStringExtra(ConstantValues.NEAREST_STOPS_DATA));
                    Log.v(TAG, "parsed stops size:" + stops.size());
                    Collections.sort(stops, new DistanceToUserComparator(userPosition));
                    for (int i = 0; i < stops.size(); i++) {
                        if (i == 0 && select) {
                            createStopMarker(stops.get(i), true, type, true);
                            textViewNearestStop.setText(stops.get(0).getStopName());
                        } else
                            createStopMarker(stops.get(i), false, type, true);
                    }
                    select = false;

                }
                searchChangeStops();

            }
        }
    }

    private void searchChangeStops() {
        long startTime = Calendar.getInstance().getTimeInMillis();
        Log.v(TAG, "searchChangeStops starts:" + startTime);
       /* for (int i = 0; i < mMapView.getOverlays().size(); i++) {
            if (mMapView.getOverlays().get(i) instanceof StopMarker) {
                if (mMapView.getBoundingBox().contains(((StopMarker) mMapView.getOverlays().get(i)).getPosition())) {
//                    Log.v(TAG, "marker:" + ((StopMarker) mMapView.getOverlays().get(i)).getStop().getStopName() + " is visible on map");
                    if (routesChoosenByUser.size() > 0) {
                        for (int j = 0; j < routes.get(routesChoosenByUser.get(0)).getStopsList().size(); j++) {
                            if (!((StopMarker) mMapView.getOverlays().get(i)).getType().equals(ConstantValues.STOP_ON_ROUTE)) {
                                GeoPoint stopGeoPoint = new GeoPoint(routes.get(routesChoosenByUser.get(0)).getStopsList().get(j).getLat(), routes.get(routesChoosenByUser.get(0)).getStopsList().get(j).getLon());
                                int distance = ((StopMarker) mMapView.getOverlays().get(i)).getPosition().distanceTo(stopGeoPoint);
                                //Log.v(TAG, "distance from stop marker:" + ((StopMarker) mMapView.getOverlays().get(i)).getStop().getStopName() + " to stop on route:" + routes.get(routesChoosenByUser.get(0)).getStopsList().get(j).getStopName() + " is:" + distance);
                                if (distance > 0 && distance < CHANGE_STOP_DISTANCE) {
                                    //Log.v(TAG, "change type of stop:" + ((StopMarker) mMapView.getOverlays().get(i)).getStop().getStopName() + " for change type");
//                                    createStopMarker(((StopMarker) mMapView.getOverlays().get(i)).getStop(), false, ConstantValues.STOP_CHANGE, false);
                                    Drawable icon = getChangeStopMarkerDrawable((StopMarker) mMapView.getOverlays().get(i));
                                    changeMarkerIcon((StopMarker) mMapView.getOverlays().get(i), icon, ConstantValues.STOP_CHANGE);
                                    changesStopList.add((StopMarker) mMapView.getOverlays().get(i));
                                }
                            }

                        }
                    }
                }
            }
        }*/
        for (int i=0; i<allStopList.size(); i++){
            if (mMapView.getBoundingBox().contains((allStopList.get(i)).getPosition())) {
//                    Log.v(TAG, "marker:" + ((StopMarker) mMapView.getOverlays().get(i)).getStop().getStopName() + " is visible on map");
                if (routesChoosenByUser.size() > 0 && routes.size() > routesChoosenByUser.get(0)) {
                    for (int j = 0; j < routes.get(routesChoosenByUser.get(0)).getStopsList().size(); j++) {
                        if (!(allStopList.get(i)).getType().equals(ConstantValues.STOP_ON_ROUTE)) {
                            GeoPoint stopGeoPoint = new GeoPoint(routes.get(routesChoosenByUser.get(0)).getStopsList().get(j).getLat(), routes.get(routesChoosenByUser.get(0)).getStopsList().get(j).getLon());
                            int distance = (allStopList.get(i)).getPosition().distanceTo(stopGeoPoint);
                            //Log.v(TAG, "distance from stop marker:" + ((StopMarker) mMapView.getOverlays().get(i)).getStop().getStopName() + " to stop on route:" + routes.get(routesChoosenByUser.get(0)).getStopsList().get(j).getStopName() + " is:" + distance);
                            if (distance > 0 && distance < CHANGE_STOP_DISTANCE) {
                                //Log.v(TAG, "change type of stop:" + ((StopMarker) mMapView.getOverlays().get(i)).getStop().getStopName() + " for change type");
//                                    createStopMarker(((StopMarker) mMapView.getOverlays().get(i)).getStop(), false, ConstantValues.STOP_CHANGE, false);
                                Drawable icon = getChangeStopMarkerDrawable(allStopList.get(i));
                                allStopList.set(i,  changeMarkerIcon(allStopList.get(i), icon, ConstantValues.STOP_CHANGE));
                                changesStopList.add(allStopList.get(i));
                            }
                        }

                    }
                }
            }
        }
        scaleMarkers(mMapView.getZoomLevel());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMapView.invalidate();
            }
        });
        Log.v(TAG, "searchChangeStops takes duration:" + (Calendar.getInstance().getTimeInMillis() - startTime));
    }

    private void searchStopsOnRoute(ArrayList<Stop> stops) {
        long startTime = Calendar.getInstance().getTimeInMillis();
        Log.v(TAG, "searchStopsOnRoute starts:" + startTime);
        for (int i=0; i< stops.size(); i++){
            for (int j=0; j<allStopList.size(); j++){
                if (stops.get(i).getStopName().equals(allStopList.get(j).getStop().getStopName())){
                    Drawable icon  = null;
                    if (allStopList.get(j).getStop().getStopType()[0] == STOP_TYPE_BUS) {
                        icon = busBlueIcon;
                    } else if (allStopList.get(j).getStop().getStopType()[0] == STOP_TYPE_TRAM) {
                        icon = tramBlueIcon;
                    } else if (allStopList.get(j).getStop().getStopType()[0] == STOP_TYPE_SKM) {
                        icon = skmBlueIcon;
                    } else if (allStopList.get(j).getStop().getStopType()[0] == STOP_TYPE_METRO) {
                        icon = metroBlueIcon;
                    }

                    onRouteStopList.add(changeMarkerIcon(allStopList.get(j), icon, ConstantValues.STOP_ON_ROUTE));
                }
            }
        }
        Log.v(TAG, "searchStopsOnRoute takes duration:" + (Calendar.getInstance().getTimeInMillis() - startTime));
    }

    private void clearStopsOnRoute(){
        Log.v(TAG, "clear stop on Route size:" + onRouteStopList.size());
        for (int i=0; i<onRouteStopList.size(); i++){
            Drawable icon  = null;
            icon = getDefaultMarkerDrawable((onRouteStopList.get(i)));
            changeMarkerIcon(onRouteStopList.get(i), icon, ConstantValues.STOP_NORMAL);
        }
        onRouteStopList.clear();
    }

    private void clearAllChangeStops() {
        long startTime = Calendar.getInstance().getTimeInMillis();
        Log.v(TAG, "clearAllChangeStops starts:" + startTime);
        for (int i=0; i<changesStopList.size(); i++){
            Drawable icon = getDefaultMarkerDrawable((changesStopList.get(i)));
                changeMarkerIcon(changesStopList.get(i), icon, ConstantValues.STOP_NORMAL);
        }
        changesStopList.clear();
        Log.v(TAG, "clearAllChangeStops takes duration:" + (Calendar.getInstance().getTimeInMillis() - startTime));

    }

    private StopMarker changeMarkerIcon(StopMarker marker, Drawable icon, String type) {
        marker.setMarkerIcon(icon);
        marker.setType(type);
        return marker;
    }

    private Drawable getDefaultMarkerDrawable(StopMarker marker) {
        Drawable icon = null;
        if (marker.getStop().getStopType()[0] == STOP_TYPE_BUS) {
            icon = busGrayIcon;
        } else if (marker.getStop().getStopType()[0] == STOP_TYPE_TRAM) {
            icon = tramGrayIcon;

        } else if (marker.getStop().getStopType()[0] == STOP_TYPE_SKM) {
            icon = skmGrayIcon;
        } else if (marker.getStop().getStopType()[0] == STOP_TYPE_METRO) {
            icon = metroGrayIcon;
        }
        return icon;
    }

    private Drawable getChangeStopMarkerDrawable(StopMarker marker) {
        Drawable icon = null;
        if (marker.getStop().getStopType()[0] == STOP_TYPE_BUS) {
            if (marker.getType().equals(ConstantValues.STOP_CHANGE))
                icon = busBlue2Icon;
        } else if (marker.getStop().getStopType()[0] == STOP_TYPE_TRAM) {
            if (marker.getType().equals(ConstantValues.STOP_CHANGE))
                icon = tramBlue2Icon;

        } else if (marker.getStop().getStopType()[0] == STOP_TYPE_SKM) {
            if (marker.getType().equals(ConstantValues.STOP_NORMAL))
                icon = skmBlue2Icon;
        } else if (marker.getStop().getStopType()[0] == STOP_TYPE_METRO) {
            if (marker.getType().equals(ConstantValues.STOP_NORMAL))
                icon = metroBlue2Icon;
        }
        return icon;
    }

    private void createStopMarker(Stop stop, boolean searchRoute, String type, boolean newMarker) {
        Drawable icon = null;
        icon = getDrawable(stop.getStopType()[0], type, icon);

        StopMarker stopMarker = new StopMarker(mMapView, stop, MapActivityTwo.this, icon);
        String finalType = type;

        stopMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                Log.v(TAG, "you click on marker:" + marker.getTitle());
                clearPreviousSelection();
                new ZtmRouteDownloader(MapActivityTwo.this).execute(marker.getSnippet());
                textViewNearestStop.setText(marker.getTitle());
                return false;
            }
        });

        if (searchRoute) {
            Log.v(TAG, "search route true - ZtmRouteDownloader");
            finalType = ConstantValues.STOP_MAIN;
            new ZtmRouteDownloader(MapActivityTwo.this).execute(stop.getStopId());
            if (stop.getStopType()[0] == STOP_TYPE_BUS) {
                icon = busBlueIcon;
            } else if (stop.getStopType()[0] == STOP_TYPE_TRAM) {
                icon = tramBlueIcon;
            } else if (stop.getStopType()[0] == STOP_TYPE_SKM) {
                icon = skmBlueIcon;
            } else if (stop.getStopType()[0] == STOP_TYPE_METRO) {
                icon = metroBlueIcon;
            }
        }

        boolean stopAddedToMap = false;
        for (int i = 0; i < mMapView.getOverlays().size(); i++) {
            if (mMapView.getOverlays().get(i) instanceof StopMarker) {
                if (((StopMarker) mMapView.getOverlays().get(i)).getPosition().equals(stopMarker.getPosition())) {
//                    Log.v(TAG, "marker added on this coordinates: type "+type+" coord:"+stopMarker.getPosition().toString());
                    stopAddedToMap = true;
                    if (finalType.equals(ConstantValues.STOP_MAIN)) {
                        ((StopMarker) mMapView.getOverlays().get(i)).setType(finalType);
                        ((StopMarker) mMapView.getOverlays().get(i)).setMarkerIcon(icon);
                    } else if (!type.equals(((StopMarker) mMapView.getOverlays().get(i)).getType())
                            && !newMarker
                            && !((StopMarker) mMapView.getOverlays().get(i)).getType().equals(ConstantValues.STOP_MAIN)) {
                        ((StopMarker) mMapView.getOverlays().get(i)).setMarkerIcon(icon);
                        ((StopMarker) mMapView.getOverlays().get(i)).setType(type);
                        Log.v(TAG, "marker already on map:" + stop.getStopName() + " type:" + type);
                        if (type.equals(ConstantValues.STOP_ON_ROUTE)){
                            onRouteStopList.add((StopMarker) mMapView.getOverlays().get(i));
                        }
                    }

                }
            }
        }

        if (!stopAddedToMap) {
//            Log.v(TAG, "add new marker on map:" + stop.getStopName() + " type:" + type);
            stopMarker.setStopName(textViewNearestStop);
            stopMarker.setType(type);
            allStopList.add(stopMarker);
            mMapView.getOverlays().add(0, stopMarker);
            if (type.equals(ConstantValues.STOP_ON_ROUTE)) {
                onRouteStopList.add(stopMarker);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMapView.invalidate();
                }
            });

        }

    }

    private Drawable getDrawable(int i, String type, Drawable icon) {
        if (i == STOP_TYPE_BUS) {
            if (type.equals(ConstantValues.STOP_NORMAL))
                icon = busGrayIcon;
            else if (type.equals(ConstantValues.STOP_CHANGE))
                icon = busBlue2Icon;
            else if (type.equals(ConstantValues.STOP_ON_ROUTE))
                icon = busBlueIcon;

        } else if (i == STOP_TYPE_TRAM) {
            if (type.equals(ConstantValues.STOP_NORMAL))
                icon = tramGrayIcon;
            else if (type.equals(ConstantValues.STOP_CHANGE))
                icon = tramBlue2Icon;
            else if (type.equals(ConstantValues.STOP_ON_ROUTE))
                icon = tramBlueIcon;
        } else if (i == STOP_TYPE_SKM) {
            if (type.equals(ConstantValues.STOP_NORMAL))
                icon = skmGrayIcon;
            else if (type.equals(ConstantValues.STOP_CHANGE))
                icon = skmBlue2Icon;
            else if (type.equals(ConstantValues.STOP_ON_ROUTE))
                icon = skmBlueIcon;
        } else if (i == STOP_TYPE_METRO) {
            if (type.equals(ConstantValues.STOP_NORMAL))
                icon = metroGrayIcon;
            else if (type.equals(ConstantValues.STOP_CHANGE))
                icon = metroBlue2Icon;
            else if (type.equals(ConstantValues.STOP_ON_ROUTE))
                icon = metroBlueIcon;
        }
        return icon;
    }

    private class RoutesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(ConstantValues.ROUTES_DATA)) {
                clearPreviousSelection();
                Log.v(TAG, "received data:" + intent.getStringExtra(ConstantValues.ROUTES_DATA));
                RouteParser routeParser = new RouteParser(MapActivityTwo.this);
                routes = routeParser.parseRoutes(intent.getStringExtra(ConstantValues.ROUTES_DATA));
                Collections.sort(routes, new TimeComparatorForBuses());

                if (routes.size() == 0){
                    LinearLayout LL = new LinearLayout(context);
                    LL.setOrientation(LinearLayout.VERTICAL);
                    TextView textView = new TextView(MapActivityTwo.this);
                    textView.setText("Brak rezultatów");
                    LL.addView(textView);
                    layout.addView(LL);
                }

                for (int i = routes.size() - 1; i >= 0; i--) {
                    BusDrawer busDrawer = new BusDrawer(context, mMapView, textViewNearestStop.getText().toString(), routes.get(i));
                    boolean addBusToList = true;
                    for (int x = 0; x < busesAddedToMap.size(); x++) {
                        Log.v(TAG, "compare schedule for busDrawer:" + busDrawer.getRoute().getRouteId() + " with busesAddedToMap:" + busesAddedToMap.get(x).getRoute().getRouteId()
                                + " result:" + new ScheduleComparator().compare(busDrawer.getSchedules(), busesAddedToMap.get(x).getSchedules()));
                        if (new ScheduleComparator().compare(busDrawer.getSchedules(), busesAddedToMap.get(x).getSchedules()) == 1) {
                            addBusToList = false;
                            buses.add(busesAddedToMap.get(x));
                        }
                    }
                    if (addBusToList) {
                        busesAddedToMap.add(busDrawer);
                        buses.add(busDrawer);
                    }
                    String time = routes.get(i).getTimeOfArrival().substring(routes.get(i).getTimeOfArrival().indexOf("T") + 1, routes.get(i).getTimeOfArrival().length() - 3);
                    Log.v(TAG, "time for route id:" + routes.get(i).getRouteId() + " arrival time:" + time+" trip id:"+routes.get(i).getTripId());
                    int[] stoptype = new int[1];
                    for (int j = 0; j < routes.get(i).getStopsList().size(); j++) {
                        stoptype[0] = routes.get(i).getRouteType();
                        routes.get(i).getStopsList().get(j).setStopType(stoptype);
                        if (i == 0)
                            createStopMarker(routes.get(i).getStopsList().get(j), false, ConstantValues.STOP_ON_ROUTE, false);
                        else
                            createStopMarker(routes.get(i).getStopsList().get(j), false, ConstantValues.STOP_NORMAL, false);
                    }
                    Log.v(TAG, "add stop on route for bus:" + routes.get(i).getRouteId() + " index:" + i);


                    ArrayList<WKTLinestring> wktLinestrings = routes.get(i).getCalculatedPoints();
                    ArrayList<Polyline> pathOverlays = new ArrayList<>();
                    ArrayList<Polyline> pathOverlayArrayList = new ArrayList<>();
                    ArrayList<RouteWayCoordinatesHelper> routeWayCoordinatesHelperArrayList = new ArrayList<>();
                    for (int j = 0; j < wktLinestrings.size(); j++) {
                        WKTReader wktReader = new WKTReader();
                        try {
                            Geometry geometry = wktReader.read(wktLinestrings.get(j).getLINESTRING());
                            for (int lineIndex = 0; lineIndex < geometry.getNumGeometries(); lineIndex++) {
                                Geometry lineGeometry = geometry.getGeometryN(lineIndex);
                                Coordinate[] lineCoordinates = lineGeometry.getCoordinates();
                                Polyline myPath = new Polyline(context);
                                if (i == 0) {
                                    myPath.setColor(Color.parseColor(BLUE));
                                } else {
                                    myPath.setColor(Color.parseColor(LIGHT_BLUE));
                                }
                                ArrayList<GeoPoint> geoPointsTemp = new ArrayList<>();

                                for (int index = 0; index < lineCoordinates.length; index++) {
                                    GeoPoint gPt = new GeoPoint(lineCoordinates[index].y, lineCoordinates[index].x);
                                    geoPointsTemp.add(gPt);
                                }
                                myPath.setPoints(geoPointsTemp);
                                RouteWayCoordinatesHelper routeWayCoordinatesHelper = new RouteWayCoordinatesHelper(geoPointsTemp, wktLinestrings.get(j).getName(), routes.get(i).getRouteId());
                                pathOverlayArrayList.add(myPath);
                                routeWayCoordinatesHelperArrayList.add(routeWayCoordinatesHelper);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }

                    Log.v(TAG, "add path route for bus:" + routes.get(i).getRouteId() + " index:" + i);
                    routeCoordinates.add(pathOverlayArrayList);
                    routeWayCoordinatesList.add(routeWayCoordinatesHelperArrayList);

                }
                Collections.reverse(routeCoordinates);
                Collections.reverse(buses);
                for (int i = 0; i < routes.size(); i++) {
                    for (int z = 0; z < routeCoordinates.get(i).size(); z++) {
                        mMapView.getOverlays().add(0, routeCoordinates.get(i).get(z));

                    }
                    String time = routes.get(i).getTimeOfArrival().substring(routes.get(i).getTimeOfArrival().indexOf("T") + 1, routes.get(i).getTimeOfArrival().length() - 3);
                    Log.v(TAG, "route id:" + routes.get(i).getRouteId() + " arrival time:" + time);

                    createInfoLayout(context, i, time);
                    if (i == 0) {
                        Log.v(TAG, "set color blue for bus:" + buses.get(i).getRoute().getRouteId() + " for i:" + i);
                        buses.get(i).setMarkerColor(Color.BLUE);
                        buses.get(i).setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.button_blue_128));
                        if (!buses.get(i).isDraw())
                            buses.get(i).setDraw(true);
                        if (buses.get(i).getGeoPoint() != null)
                            mMapView.getController().animateTo(buses.get(i).getGeoPoint());
                    } else {
                        Log.v(TAG, "set color gray for bus:" + buses.get(i).getRoute().getRouteId() + " for i=" + i);
                        buses.get(i).setMarkerColor(Color.GRAY);
                        buses.get(i).setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.button_blue2_128));
                        if (!buses.get(i).isDraw())
                            buses.get(i).setDraw(true);
                    }
                }
                searchChangeStops();
            }
        }
    }

    private void createInfoLayout(final Context context, int i, String time) {
        Log.v(TAG, "createInfoLayout i=" + i + " routeId:" + routes.get(i).getRouteId());
        LinearLayout LL = new LinearLayout(context);
        LL.setOrientation(LinearLayout.VERTICAL);

        final CheckBox checkBox = new CheckBox(MapActivityTwo.this);
//        checkBox.setId(Integer.parseInt(routes.get(i).getTripId()));
        checkBox.setId(random.nextInt(10000));
        checkBox.setTextSize(20);
        try {
            if (Integer.parseInt(routes.get(i).getRouteId()) >= 500 && Integer.parseInt(routes.get(i).getRouteId()) < 600) {
                checkBox.setTextColor(Color.RED);
            } else {
                checkBox.setTextColor(Color.BLACK);
            }
        } catch (NumberFormatException ex) {

        }
        checkBox.setText(routes.get(i).getRouteId());
        checkboxIdList.add(checkBox.getId());
        checkboxList.add(checkBox);
        if (i == 0) {
            checkBox.setChecked(true);
            routesChoosenByUser.add(i);
        } else
            checkBox.setChecked(false);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v(TAG, "on checked change:");
                Log.v(TAG, "button id:" + buttonView.getId() + " is Checked:" + isChecked);
                long startTime = Calendar.getInstance().getTimeInMillis();
                Log.v(TAG, "measure start time for:" +  routes.get(checkboxIdList.indexOf(buttonView.getId())).getRouteId());

               /* for (int j = 0; j < buses.size(); j++) {
                    for (int k=0; k<buses.get(j).getSchedules().size(); k++){
                        Log.v(TAG, "bus:"+buses.get(j).getRoute().getRouteId()+" trip id:" + buses.get(j).getRoute().getTripId()+ " time:"+buses.get(j).getSchedules().get(k).getTime().toString()+" is on stop:"+buses.get(j).getSchedules().get(k).getStop().getStopName());
                    }
                }*/
                clearAllChangeStops();
                clearStopsOnRoute();

                if (isChecked) {
                    Log.v(TAG, "choosen index:" + checkboxIdList.indexOf(buttonView.getId()));
                    Log.v(TAG, "show route ID:" + routes.get(checkboxIdList.indexOf(buttonView.getId())).getRouteId());
                    Log.v(TAG, "checkbox list:" + checkboxList.size());
                    for (int i = 0; i < checkboxList.size(); i++) {
                        Log.v(TAG, "try to unselect checkbox:" + i);
                        if (i != checkboxIdList.indexOf(buttonView.getId())) {
                            if (checkboxList.get(i).isChecked())
                                checkboxList.get(i).setChecked(false);
                        }
                    }

                    routesChoosenByUser.add(checkboxIdList.indexOf(buttonView.getId()));
                    buses.get(checkboxIdList.indexOf(buttonView.getId())).setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.button_blue_128));
                    if (buses.get(checkboxIdList.indexOf(buttonView.getId())).getBusMarker().getPosition() != null) {
                        mMapView.getController().animateTo(buses.get(checkboxIdList.indexOf(buttonView.getId())).getBusMarker().getPosition());
                    }
                    else {
                        Log.v(TAG, "position of bus:"+buses.get(checkboxIdList.indexOf(buttonView.getId())).getRoute().getRouteId()+" is null");
                    }
                    for (int z = 0; z < routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).size(); z++) {
                        routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z).setColor(Color.parseColor(BLUE));
                    }


                    if (routesChoosenByUser.size() == 0) {
                        Log.v(TAG, "nothing selected:");
                    } else {
                        searchChangeStops();
                        searchStopsOnRoute(routes.get(routesChoosenByUser.get(0)).getStopsList());
//                        for (int j = 0; j < routes.get(routesChoosenByUser.get(0)).getStopsList().size(); j++) {
//                            createStopMarker(routes.get(routesChoosenByUser.get(0)).getStopsList().get(j), false, ConstantValues.STOP_ON_ROUTE, false);
//                        }


                    }
                   /* int count = 0;
                    for (int y = 0; y<mMapView.getOverlays().size(); y++){
                        if (mMapView.getOverlays().get(y) instanceof Polyline)
                            count++;
                    }

                    for (int z = 0; z < routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).size(); z++) {
                        //routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z).setColor(Color.parseColor(BLUE));
                        if (mMapView.getOverlays().contains(routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z))) {
                            mMapView.getOverlays().remove(routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z));
                        }
                    }
                    for (int z = 0; z < routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).size(); z++) {
                        routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z).setColor(Color.parseColor(BLUE));
                        mMapView.getOverlays().add(count,routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z));
                    }*/
                } else {
                    Log.v(TAG, "choosen index:" + checkboxIdList.indexOf(buttonView.getId()));
                    Log.v(TAG, "dissmis route ID:" + routes.get(checkboxIdList.indexOf(buttonView.getId())).getRouteId());

                    routesChoosenByUser.remove(routesChoosenByUser.indexOf(checkboxIdList.indexOf(buttonView.getId())));
                    for (int z = 0; z < routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).size(); z++) {
                        routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z).setColor(Color.parseColor(LIGHT_BLUE));
                    }
                    buses.get(checkboxIdList.indexOf(buttonView.getId())).setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.button_blue2_128));

                }


                Log.v(TAG, "measure difference time:" + (Calendar.getInstance().getTimeInMillis() - startTime)+" for bus:"+routes.get(checkboxIdList.indexOf(buttonView.getId())).getRouteId());
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scaleBusMarker(mMapView.getZoomLevel());
                        scaleMarkers(mMapView.getZoomLevel());
                    }
                });*/
            }
        });


        TextView timeView = new TextView(context);
        timeView.setText(time);
        timeView.setTextColor(Color.BLACK);
        timeView.setGravity(Gravity.CENTER);
        LL.addView(checkBox);
        LL.addView(timeView);
        layout.addView(LL);

    }


    private void clearPreviousSelection() {
        Log.v(TAG, "clear previous selection");
        for (int i = 0; i < routeCoordinates.size(); i++) {
            for (int j = 0; j < routeCoordinates.get(i).size(); j++) {
//                routeCoordinates.get(i).get(j).setColor(Color.parseColor(LIGHT_BLUE));
                mMapView.getOverlays().remove(routeCoordinates.get(i).get(j));
            }

        }

        for (int i = 0; i < mMapView.getOverlays().size(); i++) {
            if (mMapView.getOverlays().get(i) instanceof StopMarker) {
                Drawable icon = getDefaultMarkerDrawable((StopMarker) mMapView.getOverlays().get(i));
                changeMarkerIcon((StopMarker) mMapView.getOverlays().get(i), icon, ConstantValues.STOP_NORMAL);
            }
        }
        for (int i = 0; i < busesAddedToMap.size(); i++) {
            busesAddedToMap.get(i).setREFRESH_IN_SEC(5);
            busesAddedToMap.get(i).setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button_gray_128));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMapView.invalidate();
                layout.removeAllViews();
            }
        });
        routesChoosenByUser.clear();
        routeCoordinates.clear();
        checkboxIdList.clear();
        buses.clear();
        checkboxList.clear();
        routes.clear();
    }

    private void registerBroadcasts() {
        nearestStopReceiver = new NearestStopReceiver();
        IntentFilter intentFilter = new IntentFilter(ConstantValues.NEAREST_STOPS_ACTION);
        registerReceiver(nearestStopReceiver, intentFilter);

        routesReceiver = new RoutesReceiver();
        IntentFilter intentFilter2 = new IntentFilter(ConstantValues.ROUTES_ACTION);
        registerReceiver(routesReceiver, intentFilter2);

    }

    private void unregisterBroadcasts() {
        unregisterReceiver(nearestStopReceiver);
        unregisterReceiver(routesReceiver);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        String message = "osmdroid permissions:";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            message += "\nLocation to show user location.";
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            message += "\nStorage access to store map tiles.";
        }
        if (!permissions.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } // else: We already have permissions, so handle as normal
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                Boolean location = perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (location && storage) {
                    // All Permissions Granted
                    Toast.makeText(MapActivityTwo.this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else if (location) {
                    Toast.makeText(this, "Storage permission is required to store map tiles to reduce data usage and for offline usage.", Toast.LENGTH_LONG).show();
                } else if (storage) {
                    Toast.makeText(this, "Location permission is required to show the user's location on map.", Toast.LENGTH_LONG).show();
                } else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(MapActivityTwo.this, "Storage permission is required to store map tiles to reduce data usage and for offline usage." +
                            "\nLocation permission is required to show the user's location on map.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
