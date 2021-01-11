package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.collections.MarkerManager;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String TAG = "MainActivity";
    public static final String SERVICE_RECEIVER = "com.example.trafficmonitor.MainActivity.RECEIVE_SERVICE";
    private Intent _m_serviceIntent;
    private MainActivity.ClientReceiver clientReceiver;

    private LocationRequest _m_locationRequest;
    private FusedLocationProviderClient _m_fusedLocationProviderClient;
    private static MainActivity _s_instance;

    // Traffic light timer parameters
    HashMap<String, Integer> _m_imageResource_trafficLights;
    private static Context _s_context;
    private HashMap<String, Integer> _m_idDictionary;
    private List<String> _m_currentState_trafficLights;


    // GPS info parameters
    private TextView _m_textView_gpsInfo;
    private UTMLocation _m_current_utm_location;
    private float _m_current_speed; //in m/s

    // map, spat info parameters
    private int _m_timeLeft_cal_neceSpeed = 25; // second
    private TextView _m_textView_mapInfo;
    private MapInfo _m_mapInfo;
    private Spat _m_spat;
    private UTMLocation _m_intersection_location;

    private ImageButton[] _m_imageButtons = new ImageButton[3];
    private ImageButton _m_imageButton_unfocus;
    private int[] _m_imageButton_id = {R.id.mainImageButtonCar, R.id.mainImageButtonBicycle, R.id.mainImageButtonWalking};
    private String[] _m_modes = {"vehicle", "bikeLane", "bikeLane"};
    private String _m_mode_selected;
    private JSONfromKML _m_lanes;
    private GoogleMap _m_gMap;
    private List<Lane> _m_trafficLights;
    private Location _m_location;
    private List<UTMLocation> _m_locationList;
    private boolean _m_determinated;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _m_serviceIntent = new Intent(this, ClientService.class);
        clientReceiver = new MainActivity.ClientReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SERVICE_RECEIVER);
        registerReceiver(clientReceiver, intentFilter);
        Bundle req = new Bundle();
        req.putString("task", "getSpatJson");
        _m_serviceIntent.putExtras(req);
        startService(_m_serviceIntent);

        Dexter.withContext(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                    startLocationRequest();
                }
                @Override
                public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                    Toast.makeText(MainActivity.this, "You need to accept Location request", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                }
            }).check();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Traffic Monitor");
        //ActionBar ab = getSupportActionBar();
        //ab.setDisplayHomeAsUpEnabled(true);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        for(int i = 0; i < _m_imageButtons.length; i++){
            _m_imageButtons[i] = findViewById(_m_imageButton_id[i]);
            _m_imageButtons[i].setBackgroundColor(Color.rgb(232, 232, 232));
            _m_imageButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()){
                        case R.id.mainImageButtonCar :
                            setFocus(_m_imageButton_unfocus, _m_imageButtons[0]);
                            _m_mode_selected = _m_modes[0];
                            break;

                        case R.id.mainImageButtonBicycle :
                            setFocus(_m_imageButton_unfocus, _m_imageButtons[1]);
                            _m_mode_selected = _m_modes[1];
                            break;

                        case R.id.mainImageButtonWalking :
                            setFocus(_m_imageButton_unfocus, _m_imageButtons[2]);
                            _m_mode_selected = _m_modes[2];
                            break;
                    }
                }
            });
        }
        // init car imageButton selected
        _m_imageButton_unfocus = _m_imageButtons[0];
        setFocus(_m_imageButton_unfocus, _m_imageButtons[1]);
        _m_mode_selected = _m_modes[1];

        _s_instance = this;
        _m_locationList = new ArrayList<>();
        _m_determinated = false;

        _s_context = this.getApplicationContext();
        _m_idDictionary = new HashMap<>();
        _m_imageResource_trafficLights = new HashMap<>();
        _m_imageResource_trafficLights.put("STOP_AND_REMAIN", R.drawable.red);
        _m_imageResource_trafficLights.put("PRE_MOVEMENT", R.drawable.red_yellow);
        _m_imageResource_trafficLights.put("PROTECTED_MOVEMENT_ALLOWED", R.drawable.green);
        _m_imageResource_trafficLights.put("PROTECTED_CLEARANCE", R.drawable.yellow);
        _m_imageResource_trafficLights.put("DARK", R.drawable.dark);
        _m_currentState_trafficLights = new ArrayList<>();



        // map, spat info
        updateSpat();
        getLanesInfo();
    }

    @Override
    public void onDestroy() {
        stopService(_m_serviceIntent);
        unregisterReceiver(clientReceiver);
        super.onDestroy();
    }

    public class ClientReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (SERVICE_RECEIVER.equals(action)) {
                    Bundle resp = intent.getExtras();
                    String task = (String) resp.get("task");
                    String value = (String) resp.get("value");
                    Log.d(TAG, task+":"+value);
                    switch (task) {
                        case "getSpatJsonResp":
                            if(Boolean.valueOf(value)){
                                Toast.makeText(MainActivity.this, "Get Spat Json Successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Invalid Authorization or Server down", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        _m_gMap = googleMap;
        //LatLng intersection = new LatLng(52.564232999999994, 13.327774999999999);
        //gMap.addMarker(new MarkerOptions().position(intersection).title("Reference Point of Intersection 14052"));
        //gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(intersection, 19f));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        _m_gMap.setMyLocationEnabled(true);
        MapInfo mapInfo = getMapInfo();
        if(mapInfo != null){
            _m_trafficLights =  mapInfo.map.intersection.lanes;
        }
        if(_m_trafficLights != null){
            //showTrafficLights();
        }
    }

    public static MainActivity getInstance() {
        return _s_instance;
    }

    public static Context getContext() {
        return _s_context;
    }

    private void startLocationRequest() {
        buildLocationRequest();
        _m_fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        _m_fusedLocationProviderClient.requestLocationUpdates(_m_locationRequest, getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationReceiver.class);
        intent.setAction(LocationReceiver.ACTION_PROCESS_UPDATE);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /*
     * Input: none
     * Return: none
     * Description: set the time interval and distance interval of request
     */
    private void buildLocationRequest() {
        _m_locationRequest = new LocationRequest();
        _m_locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        _m_locationRequest.setInterval(1000); // in milliseconds (1000 ms = 1s)
        _m_locationRequest.setFastestInterval(1000); // in milliseconds (1000 ms = 1s)
        _m_locationRequest.setSmallestDisplacement(0.5f); // in meters
    }

    /*
     * Input: buttons to cancel focusing and focus
     * Return: none
     * Description: set focused button in button group of mode
     */
    private void setFocus(ImageButton btn_unfocus, ImageButton btn_focus){
        btn_unfocus.setBackgroundColor(Color.rgb(232, 232, 232));
        btn_focus.setBackgroundColor(Color.rgb(181, 181, 181));
        _m_imageButton_unfocus = btn_focus;
    }


    /*
     * Input: none
     * Return: none
     * Description: get lanes info from assets json file and parse it into object
     */
    public void getLanesInfo(){
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    InputStream inputStream = getAssets().open("14052_lanes.json");
                    InputStreamReader isReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(isReader);
                    StringBuffer sb = new StringBuffer();
                    String str;
                    while((str = reader.readLine())!= null){
                        sb.append(str);
                    }
                    _m_lanes = Utils.laneParser(sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /*
     * Input: integer of lane id
     * Return: integer of signal group id
     * Description: get corresponding signal group of one lane
     */
    public Integer getSignalGroupOfLane(int laneId){
        int signalGroupId = 0;
        for(Lane lane : _m_trafficLights){
            if(lane.id == laneId){
                signalGroupId = lane.signalGroup;
            }
        }
        return signalGroupId;
    }

    /*
     * Input: Location object from LocationService
     * Return: none
     * Description: get current location information from LocationService,
     *              move camera of map view to current location,
     *              get street information
     *              add speed information on the map
     *              add travel trajectory on the map
     */
    public void updateLocationWGS(Location location) throws IOException {
        LatLng locationCurrent = new LatLng(location.getLatitude(), location.getLongitude());
        float speedCurrent = (float) (location.getSpeed() * 3.6); //in km/h
        if(_m_gMap != null) {
            _m_gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationCurrent, 19f));
            //add speed information on the current location

            //Log.i("Current Speed", String.format("%.1f km/h",speedCurrent));
            //Marker markerCurrent = _m_gMap.addMarker(new MarkerOptions()
            //        .position(locationCurrent).alpha(0.0f)
            //        .title(String.format("%.1f km/h",speedCurrent)));
            //markerCurrent.showInfoWindow();


            //add red trajectory line on the map
            if (_m_location !=null){
                Polyline line = _m_gMap.addPolyline(new PolylineOptions()
                        .add(locationCurrent, new LatLng(_m_location.getLatitude(),_m_location.getLongitude()))
                        .width(5)
                        .color(Color.RED));
            }
            _m_location=location;
        }
    }

    /*
     * Input: UTMLocation object
     * Return: none
     * Description: update distance to intersection called by LocationService,
     *              call determination function when distance is less than threshold
     */
    public void updateDistanceToIntersection(UTMLocation currentLocation){
        UTMLocation intersectionLocation = getIntersectionUTMLocation();
        if(intersectionLocation != null){
            double distance = Utils.getUTMDistance(currentLocation, intersectionLocation);
            if(!_m_determinated){
                if((int)distance >= 30  && (int)distance < 70){
                    _m_locationList.add(currentLocation);
                } else if ((int)distance < 30){
                    doDetermination(currentLocation);
                }
            } else {
                if ((int)distance > 30){
                    _m_determinated = false;
                }
            }
        }
    }

    /*
     * Input: list of location in the past time
     * Return: corresponding signal group
     * Description: do determination job with linear regression
     */
    public void doDeterminationLinearRegression(){
        _m_determinated = true;
        double alpha = Utils.getLinearRegressionAlpha(_m_locationList);
        double beta = Utils.getLinearRegressionBeta(_m_locationList, alpha);
        Log.i("Determination","Linear Regression Alpha Beta: "+alpha+","+beta);
        int laneId = 0;
        int signalGroupId = 0;
        double deltaABS = Double.POSITIVE_INFINITY;
        for (Lane trafficLight : _m_trafficLights){
            if(trafficLight.id == 1 || trafficLight.id == 2 || trafficLight.id == 3 || trafficLight.id == 100){
                double X = trafficLight.positionUTM.east;
                double Y = trafficLight.positionUTM.east;
                double delta = Y - (alpha * X + beta);
                if(Math.abs(delta) < deltaABS){
                    deltaABS = Math.abs(delta);
                    laneId = trafficLight.id;
                    signalGroupId = trafficLight.signalGroup;
                }
                Log.i("Determination","Result lane ID: "+trafficLight.id+", signal group ID:"+trafficLight.signalGroup+"delta: "+delta);

            }
        }
    }

    /*
     * Input: list of location in the past time
     * Return: corresponding signal group
     * Description: do determination job with distance between user location and lane points
     */
    public void doDetermination(UTMLocation currentLocation){
        _m_determinated = true;
        int determinatedLaneId = 0;
        double determinatedDistance = Double.POSITIVE_INFINITY;
        for( Feature feature : _m_lanes.features){
            int laneId = feature.properties.laneId;
            String laneType = feature.properties.laneType;
            double minDistance = Double.POSITIVE_INFINITY;
            if(laneType.equals(_m_mode_selected)){
                for(UTMLocation location : Utils.addPointsToLanes(feature.geometry.coordinates)){
                    double distance = Utils.getUTMDistance(currentLocation, location);
                    if(distance < minDistance){
                        minDistance = distance;
                    }
                }

                if(minDistance < determinatedDistance){
                    determinatedDistance = minDistance;
                    determinatedLaneId = laneId;
                }
            }
        }
        int determinatedSignalGroupId = getSignalGroupOfLane(determinatedLaneId);
        String determinationResult = "Lane: " + determinatedLaneId + " Signal Group: " +determinatedSignalGroupId;
        Toast.makeText(MainActivity.this, determinationResult, Toast.LENGTH_SHORT).show();
        //Log.i("Determination", "Result lane ID: "+determinatedLaneId+" min distance: "+determinatedDistance+" m");
    }

    /*
     * Input: list of traffic lights states
     * Return: none
     * Description: show traffic lights with their positions and states at map view
     */
    public void showTrafficLights(){
        MarkerManager markerManager = new MarkerManager(_m_gMap);
        MarkerManager.Collection markerCollection = markerManager.newCollection();
        for(int i = 0; i < _m_trafficLights.size(); i++){
            markerCollection.addMarker(
                    new MarkerOptions()
                            .position(
                                    new LatLng(
                                            _m_trafficLights.get(i).positionWGS84.lat,
                                            _m_trafficLights.get(i).positionWGS84.lng
                                    )
                            )
                            .icon(BitmapDescriptorFactory.fromBitmap(getTrafficLightBitmap("STOP_AND_REMAIN")))
                            .title("nameTest")
            );
        }
    }

    /*
     * Input: String of states
     * Return: Bitmap of traffic lights
     * Description: get corresponding bitmaps of traffic lights as icon
     */
    @SuppressLint("ResourceType")
    public Bitmap getTrafficLightBitmap(String state){
        InputStream is;
        switch (state){
            default:
                is = getResources().openRawResource(R.drawable.red);
                break;
        }
        Bitmap bm = BitmapFactory.decodeStream(is);
        // before return, bitmap was scaled
        return Bitmap.createScaledBitmap(bm, bm.getWidth()/2, bm.getHeight()/2,true);
    }

    // Traffic light timer functions
    /*
     * Input: none
     * Return: none
     * Description: start timer and update TextView, ImageView of traffic lights
     */
    private void timer(int signalGroupId) {
        long timeLeft;
        String state;
        String[] result = calculateCurrentState(signalGroupId).split(":");
        state = result[0];
        timeLeft = Long.valueOf(result[1]);
        //updateTrafficLight
        ImageView imageView = (ImageView) findViewById(_m_idDictionary.get("trafficLight"+signalGroupId));
        imageView.setImageResource(_m_imageResource_trafficLights.get(state));
        new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                //updateCountDownText
                int seconds = (int) millisUntilFinished / 1000;
                String timeLeftFormatted = String.format(Locale.getDefault(), "%02d", seconds);
                TextView textView = (TextView) findViewById(_m_idDictionary.get("timer"+signalGroupId));
                textView.setText(timeLeftFormatted);
            }
            @Override
            public void onFinish() {
                timer(signalGroupId);
            }
        }.start();
    }

    /*
     * Input: int number of signalGroupId
     * Return: none
     * Description: calculate current state of a traffic light
     */
    private String calculateCurrentState(int signalGroupId){
        int firstStateLikelyTime = _m_spat.intersectionStates.get(0).movementStates.get(signalGroupId-1).movementEvents.get(0).timeChange.likelyTime / 10;
        List<String> stateSequence = new ArrayList<>();
        List<Integer> relativeLikelyTime = new ArrayList<>();
        stateSequence.add(_m_spat.intersectionStates.get(0).movementStates.get(signalGroupId-1).movementEvents.get(0).phaseState);
        relativeLikelyTime.add(0);
        int circulationTime = 0;
        for(int j = 1; j < _m_spat.intersectionStates.get(0).movementStates.get(signalGroupId-1).movementEvents.size(); j++){
            MovementEvent movementEvent = _m_spat.intersectionStates.get(0).movementStates.get(signalGroupId-1).movementEvents.get(j);
            if(movementEvent.phaseState.equals(stateSequence.get(0))){
                circulationTime = movementEvent.timeChange.likelyTime/10 -firstStateLikelyTime;
                break;
            }
            stateSequence.add(movementEvent.phaseState);
            relativeLikelyTime.add(movementEvent.timeChange.likelyTime/10 - firstStateLikelyTime);
        }

        Timestamp timestamp = new Timestamp(new Date().getTime());
        //second in the current hour
        int time = timestamp.getMinutes() * 60 + timestamp.getSeconds();
        String state = stateSequence.get(0);
        int timeLefts = 0;
        int modulo = (int) ((time - firstStateLikelyTime) % circulationTime);
        List<List<Integer>> thresholds = new ArrayList<>();
        for(int i = 0; i < stateSequence.size(); i++){
            List<Integer> threshold = new ArrayList<>();
            threshold.add(relativeLikelyTime.get(i));
            if(i < stateSequence.size()-1) {
                threshold.add(relativeLikelyTime.get(i+1));
            } else {
                threshold.add(circulationTime);
            }
            thresholds.add(threshold);
        }
        if(modulo >= 0){
            for(int i = 0; i < thresholds.size(); i++){
                if(i < thresholds.size()-1) {
                    if(modulo >= thresholds.get(i).get(0) && modulo < thresholds.get(i).get(1)){
                        state = stateSequence.get(i+1);
                        timeLefts = relativeLikelyTime.get(i+1) - modulo;
                        break;
                    }
                } else {
                    state = stateSequence.get(0);
                    timeLefts = circulationTime - modulo;
                    break;
                }
            }
        } else {
            for(int i = 0; i < thresholds.size(); i++){
                if(i < thresholds.size()-1) {
                    if(modulo + circulationTime >= thresholds.get(i).get(0) && modulo + circulationTime  < thresholds.get(i).get(1)){
                        state = stateSequence.get(i+1);
                        timeLefts = relativeLikelyTime.get(i+1) - (modulo + circulationTime);
                        break;
                    }
                } else {
                    state = stateSequence.get(0);
                    timeLefts = - modulo;
                    break;
                }
            }
        }
        return state+":"+((long)timeLefts*1000);
    }

    // GPS info functions
    /*
     * Input: none
     * Return: none
     * Description: get object of mapInfo
     */
    public MapInfo getMapInfo() {
        return _m_mapInfo;
    }

    /*
     * Input: none
     * Return: none
     * Description: get object of mapInfo
     */
    public UTMLocation getIntersectionUTMLocation() {
        return _m_intersection_location;
    }

    // map, spat info functions
    /*
     * Input: none
     * Return: none
     * Description: get json of mapInfo from server, parse it into object and update MapInfo TextView
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateMapInfoText() {
        /*
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    String mapInfoStr = Utils.getMapInfoJson(getResources().getString(R.string.map_info_url));
                    _m_mapInfo = Utils.mapInfoParser(mapInfoStr);
                    int intersectionID = _m_mapInfo.map.intersection.intersectionID;
                    _m_intersection_location = new UTMLocation(_m_mapInfo.map.intersection.positionUTM.east,_m_mapInfo.map.intersection.positionUTM.north);
                    //Update view at main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            _m_textView_mapInfo.setText("Intersection ID: " + String.valueOf(intersectionID));
                        }
                    });
                } catch (Exception e) {
                    Log.w("Client", "Invalid Authorization or Server down. Please check AuthUrlInfo");
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();

         */
    }

    /*
     * Input: none
     * Return: none
     * Description: get json of spat from server,
     *              parse it into object,
     *              add ImageViews of traffic light
     *              update spat TextView with state and left Time of first signal group
     */
    private void updateSpat() {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
                try {
                    String spatStr = null; // need almost 12s
                    //spatStr = Utils.getSpatJson(getResources().getString(R.string.spat_url));
                    //_m_spat = Utils.spatParser(spatStr);
                    /*
                    List<MovementState> movementStates = _m_spat.intersectionStates.get(0).movementStates;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScrollView scrollView = findViewById(R.id.carScrollViewTrafficLight);
                            LinearLayout linearVertical = new LinearLayout(getContext());
                            linearVertical.setOrientation(LinearLayout.VERTICAL);

                            TextView textViewIntersection = new TextView(getContext());
                            textViewIntersection.setText("Intersection: " + _m_mapInfo.map.intersection.intersectionID);
                            textViewIntersection.setTextColor(Color.BLACK);
                            textViewIntersection.setTextSize(30);
                            LinearLayout.LayoutParams paramsIntersection = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                            paramsIntersection.gravity = Gravity.CENTER;
                            textViewIntersection.setLayoutParams(paramsIntersection);
                            linearVertical.addView(textViewIntersection);

                            long currentTimeMillis = System.currentTimeMillis();
                            for (int i = 0; i < movementStates.size()/3 + 1; i++) {
                                LinearLayout linearHorizontal = new LinearLayout(getContext());
                                linearHorizontal.setOrientation(LinearLayout.HORIZONTAL);
                                LinearLayout.LayoutParams paramsLinearHorizontal = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                                paramsLinearHorizontal.gravity = Gravity.CENTER;
                                linearHorizontal.setLayoutParams(paramsLinearHorizontal);
                                ImageView imageViewTLMaster = findViewById(R.id.carImageViewTrafficLight1);
                                int widthImageViewTL = imageViewTLMaster.getWidth();
                                int heightImageViewTL = imageViewTLMaster.getHeight();
                                int countTLThisLinear;
                                if (movementStates.size() > (i+1)*3){
                                    countTLThisLinear = 3;
                                } else {
                                    countTLThisLinear = movementStates.size()%3;
                                }
                                for (int j = 0; j < countTLThisLinear; j++) {
                                    LinearLayout linearTL = new LinearLayout(getContext());
                                    linearTL.setOrientation(LinearLayout.VERTICAL);
                                    LinearLayout.LayoutParams paramsLinearTL = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                                    paramsLinearTL.gravity = Gravity.CENTER;
                                    linearTL.setLayoutParams(paramsLinearTL);


                                    ImageView imageViewTL = new ImageView(getContext());
                                    currentTimeMillis++;
                                    String idTrafficLight = String.valueOf(currentTimeMillis).substring(4);
                                    _m_idDictionary.put("trafficLight"+movementStates.get(i*3+j).signalGroupId, Integer.parseInt(idTrafficLight));
                                    imageViewTL.setId(Integer.valueOf(idTrafficLight));
                                    imageViewTL.setImageResource(R.drawable.red);
                                    LinearLayout.LayoutParams paramsImageViewTL = new LinearLayout.LayoutParams(widthImageViewTL, heightImageViewTL);
                                    imageViewTL.setLayoutParams(paramsImageViewTL);
                                    linearTL.addView(imageViewTL);

                                    TextView textViewSignalGroup = new TextView(getContext());
                                    textViewSignalGroup.setText("Nr. "+ movementStates.get(i*3+j).signalGroupId);
                                    textViewSignalGroup.setTextColor(Color.BLACK);
                                    textViewSignalGroup.setTextSize(20);
                                    LinearLayout.LayoutParams paramsSignalGroup = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                                    paramsSignalGroup.gravity = Gravity.CENTER;
                                    textViewSignalGroup.setLayoutParams(paramsSignalGroup);
                                    linearTL.addView(textViewSignalGroup);

                                    TextView textViewTimer = new TextView(getContext());
                                    currentTimeMillis++;
                                    String idTimer = String.valueOf(currentTimeMillis).substring(4);
                                    _m_idDictionary.put("timer"+movementStates.get(i*3+j).signalGroupId, Integer.parseInt(idTimer));
                                    textViewTimer.setId( Integer.valueOf(idTimer));
                                    textViewTimer.setText("60");
                                    textViewTimer.setTextColor(Color.BLACK);
                                    textViewTimer.setTextSize(35);
                                    LinearLayout.LayoutParams paramsTimer = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                                    paramsTimer.gravity = Gravity.CENTER;
                                    textViewTimer.setLayoutParams(paramsTimer);
                                    linearTL.addView(textViewTimer);

                                    linearHorizontal.addView(linearTL);
                                }
                                linearVertical.addView(linearHorizontal);
                            }
                            scrollView.addView(linearVertical);
                        }
                    });
                    int size = movementStates.size();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0; i < size; i++){
                                timer(movementStates.get(i).signalGroupId);
                            }
                            findViewById(R.id.carLoadingPanel).setVisibility(View.GONE);
                        }
                    });
                    */
                } catch (Exception e) {
                    Log.w("Client", "Invalid Authorization or Server down. Please check AuthUrlInfo");
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

}
