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
import java.util.Timer;
import java.util.TimerTask;

import miklukada.pl.takdojade.R;
import miklukada.pl.takdojade.comparator.DistanceToUserComparator;
import miklukada.pl.takdojade.comparator.TimeComparatorForBuses;
import miklukada.pl.takdojade.datamodel.Route;
import miklukada.pl.takdojade.datamodel.Stop;
import miklukada.pl.takdojade.markeroverlay.StopMarker;
import miklukada.pl.takdojade.markeroverlay.StopOnRouteMarker;
import miklukada.pl.takdojade.parser.RouteParser;
import miklukada.pl.takdojade.parser.StopParser;
import miklukada.pl.takdojade.route.BusDrawer;
import miklukada.pl.takdojade.utils.ConstantValues;
import miklukada.pl.takdojade.utils.CustomResourceProxy;
import miklukada.pl.takdojade.wktdata.WKTLinestring;
import miklukada.pl.takdojade.ztmdata.RouteWayCoordinatesHelper;
import miklukada.pl.takdojade.ztmdata.ZtmRouteDownloader;
import miklukada.pl.takdojade.ztmdata.ZtmStopsDownloader;

public class MapActivity extends Activity implements MapEventsReceiver {

    private final String TAG = getClass().getSimpleName();
    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private final double DISTANCE_IN_METERS = 1500;

    public final static int STOP_TYPE_TRAM = 0;
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
    private boolean canDownload = false;
    private GeoPoint userPosition = null;
    private boolean select = false;

    private ArrayList<Route> routes = new ArrayList<>();
    private ArrayList<BusDrawer> buses = new ArrayList<>();
    private ArrayList<String> addedBusses = new ArrayList<>();
    private ArrayList<ArrayList<Polyline>> routeCoordinates = new ArrayList<>();
    private ArrayList<ArrayList<RouteWayCoordinatesHelper>> routeWayCoordinatesList = new ArrayList<>();
    private ArrayList<Integer> routesChoosenByUser = new ArrayList<>();
    final ArrayList<Integer> checkboxIdList = new ArrayList<>();
    final ArrayList<CheckBox> checkboxList = new ArrayList<>();
    private ArrayList<ArrayList<StopOnRouteMarker>> stopOnRouteMarkers = new ArrayList<>();

    private ArrayList<StopMarker> allStopsMarkers = new ArrayList<>();

    private long checkScrollTime = 0;

    private TextView debug;

    private String BLUE = "#0B04CB";

    private String LIGHT_BLUE = "#5F7CD6";

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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcasts();
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        for (int i = 0; i < mMapView.getOverlays().size(); i++) {
            if (mMapView.getOverlays().get(i) instanceof StopOnRouteMarker) {
                mMapView.getOverlays().remove(i);
            }
        }
        mMapView.invalidate();
        return false;
    }

    private void initializeMap() {

        final DisplayMetrics dm = getResources().getDisplayMetrics();

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.setMaxZoomLevel(22);

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
        mapController.setZoom(15);

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
                                            new ZtmStopsDownloader(MapActivity.this, ConstantValues.STOP_NORMAL).execute(DISTANCE_IN_METERS, mMapView.getMapCenter().getLatitude(), mMapView.getMapCenter().getLongitude());
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
                switch (event.getZoomLevel()) {
                    case 10:
                        scaleMarkers(30, 30, event.getZoomLevel());
                        break;
                    case 11:
                        scaleMarkers(40, 40, event.getZoomLevel());
                        break;
                    case 12:
                        scaleMarkers(50, 50, event.getZoomLevel());
                        break;
                    case 13:
                        scaleMarkers(60, 60, event.getZoomLevel());
                        break;
                    case 14:
                        scaleMarkers(70, 70, event.getZoomLevel());
                        break;
                    case 15:
                        scaleMarkers(80, 80, event.getZoomLevel());
                        break;
                    case 16:
                        scaleMarkers(90, 90, event.getZoomLevel());
                        break;
                    case 17:
                        scaleMarkers(100, 100, event.getZoomLevel());
                        break;
                    case 18:
                        scaleMarkers(110, 110, event.getZoomLevel());
                        break;
                    case 19:
                        scaleMarkers(120, 120, event.getZoomLevel());
                        break;
                }
                return false;
            }
        });

        myLocationNewOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
//                GeoPoint geoPoint = new GeoPoint(52.176524, 21.001212);
                userPosition = myLocationNewOverlay.getMyLocation();
                mapController.animateTo(userPosition);
                new ZtmStopsDownloader(MapActivity.this, ConstantValues.STOP_NORMAL).execute(DISTANCE_IN_METERS, userPosition.getLatitude(), userPosition.getLongitude());
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

    private void scaleMarkers(int width, int height, int zoom) {
        debug.setText("zoom:" + zoom + " marker w:" + width + " h:" + height);
        for (int i = 0; i < mMapView.getOverlays().size(); i++) {
            if (mMapView.getOverlays().get(i) instanceof StopMarker) {
                ((StopMarker) mMapView.getOverlays().get(i)).scaleMarker(width, height);
            }
        }
        for (int i = 0; i < mMapView.getOverlays().size(); i++) {
            if (mMapView.getOverlays().get(i) instanceof StopOnRouteMarker) {
                ((StopOnRouteMarker) mMapView.getOverlays().get(i)).scaleMarker(width, height);
            }
        }


        mMapView.invalidate();
    }


    private void addStopOnMap(Stop stop, boolean selectStop, String type) {
        Drawable icon = null;
        if (stop.getStopType()[0] == STOP_TYPE_BUS) {
            if (type.equals(ConstantValues.STOP_NORMAL))
                icon = getResources().getDrawable(R.drawable.pin_gray_bus_128);
            else if (type.equals(ConstantValues.STOP_CHANGE))
                icon = getResources().getDrawable(R.drawable.pin_blue2_bus_128);

        } else if (stop.getStopType()[0] == STOP_TYPE_TRAM) {
            if (type.equals(ConstantValues.STOP_NORMAL))
                icon = getResources().getDrawable(R.drawable.pin_gray_tram_128);
            else if (type.equals(ConstantValues.STOP_CHANGE))
                icon = getResources().getDrawable(R.drawable.pin_blue2_tram_128);
        } else if (stop.getStopType()[0] == STOP_TYPE_SKM) {
            if (type.equals(ConstantValues.STOP_NORMAL))
                icon = getResources().getDrawable(R.drawable.pin_gray_skm_128);
            else if (type.equals(ConstantValues.STOP_CHANGE))
                icon = getResources().getDrawable(R.drawable.pin_blue2_skm_128);
        }
        StopMarker stopMarker = new StopMarker(mMapView, stop, MapActivity.this, icon);
        stopMarker.setStopName(textViewNearestStop);
        if (selectStop) {
            stopMarker.scaleMarker(100, 100);
            new ZtmRouteDownloader(MapActivity.this).execute(stop.getStopId());
        }

        boolean stopAddedToMap = false;
        for (int i=0; i<mMapView.getOverlays().size(); i++){
            if (mMapView.getOverlays().get(i) instanceof StopMarker){
                if (((StopMarker) mMapView.getOverlays().get(i)).getPosition().equals(stopMarker.getPosition())){
                    //Log.v(TAG, "marker added on this coordinates: type "+type+" coord:"+stopMarker.getPosition().toString());
                    stopAddedToMap = true;
                }
            }
        }

        if (!stopAddedToMap) {
            Log.v(TAG, "add new marker on map:"+stop.getStopName()+" type:"+type);
            mMapView.getOverlays().add(0,stopMarker);
            mMapView.invalidate();
        } else {
            Log.v(TAG, "marker already on map:" + stop.getStopName() + " type:" + type);
        }
    }

    private StopOnRouteMarker addStopOnRoute(Stop stop, boolean stopOnSelectedRoute, String routeId) {
        Drawable icon = null;
        if (stopOnSelectedRoute) {
            if (stop.getStopType()[0] == STOP_TYPE_BUS) {
                icon = getResources().getDrawable(R.drawable.pin_blue_bus_128);
            } else if (stop.getStopType()[0] == STOP_TYPE_TRAM) {
                icon = getResources().getDrawable(R.drawable.pin_blue_tram_128);
            }
        } else {
            if (stop.getStopType()[0] == STOP_TYPE_BUS) {
                icon = getResources().getDrawable(R.drawable.pin_gray_bus_128);
            } else if (stop.getStopType()[0] == STOP_TYPE_TRAM) {
                icon = getResources().getDrawable(R.drawable.pin_gray_tram_128);
            }
        }
        StopOnRouteMarker stopOnRouteMarker = new StopOnRouteMarker(mMapView, stop, MapActivity.this, icon);
        stopOnRouteMarker.setRouteId(routeId);
        if (!mMapView.getOverlays().contains(stopOnRouteMarker)) {
            int count = 0;
            for (int i = 0; i < mMapView.getOverlays().size(); i++) {
                count++;
            }
            mMapView.getOverlays().add(count, stopOnRouteMarker);
            mMapView.invalidate();
        }
        return stopOnRouteMarker;
    }

    private class NearestStopReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "NearestStopReceiver received data:" + intent.getStringExtra(ConstantValues.NEAREST_STOPS_DATA) + " length:" + intent.getStringExtra(ConstantValues.NEAREST_STOPS_DATA).length());
            if (intent.getStringExtra(ConstantValues.NEAREST_STOPS_DATA).length() > 3) {
                String type = intent.getStringExtra(ConstantValues.STOP_TYPE);
                StopParser stopParser = new StopParser(MapActivity.this);
                stops = stopParser.getStopsList(intent.getStringExtra(ConstantValues.NEAREST_STOPS_DATA));
                Log.v(TAG, "parsed stops size:" + stops.size());
                Collections.sort(stops, new DistanceToUserComparator(userPosition));

                for (int i = 0; i < stops.size(); i++) {
                    if (i == 0 && select) {
                        addStopOnMap(stops.get(i), true, type);
                        textViewNearestStop.setText(stops.get(0).getStopName());
                    } else
                        addStopOnMap(stops.get(i), false, type);
                }
                select = false;

            }
        }
    }

    private class RoutesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(ConstantValues.ROUTES_DATA)) {
                clearPreviousSelection();
                Log.v(TAG, "received data:" + intent.getStringExtra(ConstantValues.ROUTES_DATA));
                RouteParser routeParser = new RouteParser(MapActivity.this);
                routes = routeParser.parseRoutes(intent.getStringExtra(ConstantValues.ROUTES_DATA));
                Collections.sort(routes, new TimeComparatorForBuses());


                for (int i = routes.size() - 1; i >= 0; i--) {
                    BusDrawer busDrawer = new BusDrawer(context, mMapView, textViewNearestStop.getText().toString(), routes.get(i));
                    buses.add(busDrawer);
                    String time = routes.get(i).getTimeOfArrival().substring(routes.get(i).getTimeOfArrival().indexOf("T") + 1, routes.get(i).getTimeOfArrival().length() - 3);
                    Log.v(TAG, "route id:" + routes.get(i).getRouteId() + " arrival time:" + time);
                    int[] stoptype = new int[1];
                    ArrayList<StopOnRouteMarker> tempStopList = new ArrayList<>();
                    for (int j = 0; j < routes.get(i).getStopsList().size(); j++) {
                        stoptype[0] = routes.get(i).getRouteType();
                        routes.get(i).getStopsList().get(j).setStopType(stoptype);
                        new ZtmStopsDownloader(MapActivity.this, ConstantValues.STOP_CHANGE).execute((double) 20, routes.get(i).getStopsList().get(j).getLat(), routes.get(i).getStopsList().get(j).getLon());
                        if (i == 0) {
                            tempStopList.add(addStopOnRoute(routes.get(i).getStopsList().get(j), true, routes.get(i).getRouteId()));
                        } else {
                            tempStopList.add(addStopOnRoute(routes.get(i).getStopsList().get(j), false, routes.get(i).getRouteId()));
                        }
                    }
                    Log.v(TAG, "add stop on route for bus:" + routes.get(i).getRouteId() + " index:" + i);
                    stopOnRouteMarkers.add(tempStopList);

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


//                    for (int z = 0; z < pathOverlayArrayList.size(); z++) {
//                        mMapView.getOverlays().add(pathOverlayArrayList.get(z));
//
//                    }
                    Log.v(TAG, "add path route for bus:" + routes.get(i).getRouteId() + " index:" + i);
                    routeCoordinates.add(pathOverlayArrayList);
                    routeWayCoordinatesList.add(routeWayCoordinatesHelperArrayList);

                }
                Collections.reverse(stopOnRouteMarkers);
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
                        buses.get(i).setDraw(true);
                        if (buses.get(i).getGeoPoint() != null)
                            mMapView.getController().animateTo(buses.get(i).getGeoPoint());
                    } else {
                        Log.v(TAG, "set color gray for bus:" + buses.get(i).getRoute().getRouteId() + " for i=" + i);
                        buses.get(i).setMarkerColor(Color.GRAY);
                        buses.get(i).setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.button_blue2_128));
                        buses.get(i).setDraw(true);
                    }
                }


            }
        }

        private void createInfoLayout(final Context context, int i, String time) {
            Log.v(TAG, "createInfoLayout i=" + i + " routeId:" + routes.get(i).getRouteId());
            LinearLayout LL = new LinearLayout(context);
            LL.setOrientation(LinearLayout.VERTICAL);

            final CheckBox checkBox = new CheckBox(MapActivity.this);
            checkBox.setId(Integer.parseInt(routes.get(i).getTripId()));
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
                    if (isChecked) {
                        Log.v(TAG, "choosen index:" + checkboxIdList.indexOf(buttonView.getId()));
                        Log.v(TAG, "show route ID:" + routes.get(checkboxIdList.indexOf(buttonView.getId())).getRouteId());
                        Log.v(TAG, "checkbox list:" + checkboxList.size());
                        for (int i = 0; i < checkboxList.size(); i++) {
                            Log.v(TAG, "try to unselect checkbox:" + i);
                            if (i != checkboxIdList.indexOf(buttonView.getId())) {
                                checkboxList.get(i).setChecked(false);
                            }
                        }

                        ArrayList<Polyline> tempPolylineArrayList = new ArrayList<Polyline>();
                        routesChoosenByUser.add(checkboxIdList.indexOf(buttonView.getId()));
                        for (int z = 0; z < routesChoosenByUser.size(); z++) {
                            Log.v(TAG, "routes choosen by user:" + routesChoosenByUser.get(z));
                        }

                        for (int z = 0; z < routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).size(); z++) {
                            routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z).setColor(Color.parseColor(BLUE));
                        }

                        mMapView.invalidate();
                    } else {
                        Log.v(TAG, "choosen index:" + checkboxIdList.indexOf(buttonView.getId()));
                        Log.v(TAG, "dissmis route ID:" + routes.get(checkboxIdList.indexOf(buttonView.getId())).getRouteId());

                        routesChoosenByUser.remove(routesChoosenByUser.indexOf(checkboxIdList.indexOf(buttonView.getId())));
                        for (int z = 0; z < routesChoosenByUser.size(); z++) {
                            Log.v(TAG, "routes choosen by user:" + routesChoosenByUser.get(z));
                        }
                        for (int z = 0; z < routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).size(); z++) {
                            routeCoordinates.get(checkboxIdList.indexOf(buttonView.getId())).get(z).setColor(Color.parseColor(LIGHT_BLUE));
                        }

                        mMapView.invalidate();

                    }


                    for (int i = 0; i < routeCoordinates.size(); i++) {
                        for (int j = 0; j < routeCoordinates.get(i).size(); j++) {
                            mMapView.getOverlays().remove(routeCoordinates.get(i).get(j));
                        }
                        for (int j = 0; j < stopOnRouteMarkers.get(i).size(); j++) {
                            mMapView.getOverlays().remove(stopOnRouteMarkers.get(i).get(j));
                        }
                    }
                    int stopMarkerCount = 0;
                    for (int i = 0; i < mMapView.getOverlays().size(); i++) {
                        if (mMapView.getOverlays().get(i) instanceof StopMarker) {
                            stopMarkerCount++;
                        }
                    }

                    for (int i = 0; i < routeCoordinates.size(); i++) {
                        if (!routesChoosenByUser.contains(i)) {


                            Drawable icon = null;
                            switch (routes.get(checkboxIdList.indexOf(buttonView.getId())).getRouteType()) {
                                case STOP_TYPE_BUS:
                                    icon = getResources().getDrawable(R.drawable.pin_gray_bus_128);
                                    break;
                                case STOP_TYPE_TRAM:
                                    icon = getResources().getDrawable(R.drawable.pin_gray_tram_128);
                                    break;
                            }


                            for (int j = 0; j < stopOnRouteMarkers.get(i).size(); j++) {
                                stopOnRouteMarkers.get(i).get(j).setMarkerIcon(icon);
                                mMapView.getOverlays().add(stopMarkerCount, stopOnRouteMarkers.get(i).get(j));
                            }

                            mMapView.getOverlays().remove(buses.get(i));
                            if (mMapView.getOverlays().indexOf(stopOnRouteMarkers.get(i).get(0)) > -1)
                                mMapView.getOverlays().add(mMapView.getOverlays().indexOf(stopOnRouteMarkers.get(i).get(0)), buses.get(i).getBusMarker());

                            for (int j = 0; j < routeCoordinates.get(i).size(); j++) {
                                routeCoordinates.get(i).get(j).setColor(Color.parseColor(LIGHT_BLUE));
                                mMapView.getOverlays().add(0, routeCoordinates.get(i).get(j));
                            }

                            buses.get(i).setMarkerColor(Color.parseColor(LIGHT_BLUE));
                            buses.get(i).setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.button_blue2_128));
                            Log.v(TAG, "try to draw bus at gray color:" + buses.get(i).getRoute().getRouteId());

                        } else {
                            Drawable icon = null;
                            switch (routes.get(checkboxIdList.indexOf(buttonView.getId())).getRouteType()) {
                                case STOP_TYPE_BUS:
                                    icon = getResources().getDrawable(R.drawable.pin_blue_bus_128);
                                    break;
                                case STOP_TYPE_TRAM:
                                    icon = getResources().getDrawable(R.drawable.pin_blue_tram_128);
                                    break;
                            }

                            int pathCount = 0;
                            for (int x = 0; x < mMapView.getOverlays().size(); x++) {
                                if (mMapView.getOverlays().get(x) instanceof Polyline) {
                                    pathCount++;
                                }
                            }

                            for (int j = 0; j < routeCoordinates.get(i).size(); j++) {
                                routeCoordinates.get(i).get(j).setColor(Color.parseColor(BLUE));
                                mMapView.getOverlays().add(pathCount, routeCoordinates.get(i).get(j));
                            }


                            mMapView.getOverlays().remove(buses.get(i));
                            mMapView.getOverlays().add(buses.get(i).getBusMarker());

                            for (int j = 0; j < stopOnRouteMarkers.get(i).size(); j++) {
                                stopOnRouteMarkers.get(i).get(j).setMarkerIcon(icon);
                                mMapView.getOverlays().add(stopOnRouteMarkers.get(i).get(j));
                            }

                            buses.get(i).setMarkerColor(Color.BLUE);
                            buses.get(i).setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.button_blue_128));
                            Log.v(TAG, "try to draw bus at blue color:" + buses.get(i).getRoute().getRouteId());
                            if (buses.get(i).getGeoPoint() != null)
                                mMapView.getController().animateTo(buses.get(i).getGeoPoint());


                        }
                    }

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

    }

    private void clearPreviousSelection() {
        for (int i = 0; i < routeCoordinates.size(); i++) {
            for (int j = 0; j < routeCoordinates.get(i).size(); j++) {
                routeCoordinates.get(i).get(j).setColor(Color.GRAY);
//                mMapView.getOverlays().remove(routeCoordinates.get(i).get(j));
            }
//            for (int j = 0; j < stopOnRouteMarkers.get(i).size(); j++) {
//                mMapView.getOverlays().remove(stopOnRouteMarkers.get(i).get(j));
//            }
        }
        mMapView.invalidate();
        layout.removeAllViews();
        stopOnRouteMarkers.clear();
        routesChoosenByUser.clear();
        stopOnRouteMarkers.clear();
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
                    Toast.makeText(MapActivity.this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else if (location) {
                    Toast.makeText(this, "Storage permission is required to store map tiles to reduce data usage and for offline usage.", Toast.LENGTH_LONG).show();
                } else if (storage) {
                    Toast.makeText(this, "Location permission is required to show the user's location on map.", Toast.LENGTH_LONG).show();
                } else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(MapActivity.this, "Storage permission is required to store map tiles to reduce data usage and for offline usage." +
                            "\nLocation permission is required to show the user's location on map.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
