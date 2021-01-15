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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
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
    public static final String SERVICE_RECEIVER = "com.example.trafficmonitor.RECEIVE_SERVICE";
    private Intent _m_serviceIntent;
    private MainActivity.ClientReceiver _m_clientReceiver;
    private ClientService.ClientBinder _m_clientBinder;
    private ServiceConnection _m_serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            _m_clientBinder = (ClientService.ClientBinder) service;
        }
    };

    private LocationRequest _m_locationRequest;
    private FusedLocationProviderClient _m_fusedLocationProviderClient;
    private static MainActivity _s_instance;

    // Traffic light timer parameters
    HashMap<String, Integer> _m_imageResource_trafficLights;
    private HashMap<String, Integer> _m_idDictionary;

    // map, spat info parameters
    private MapInfo _m_mapInfo;
    private UTMLocation _m_intersection_location;
    private Spat _m_spat;
    private LanesKML _m_lanes;
    private ConnectionsKML _m_connections;

    private ImageButton[] _m_imageButtons = new ImageButton[3];
    private ImageButton _m_imageButton_unfocus;
    private int[] _m_imageButton_id = {R.id.mainImageButtonCar, R.id.mainImageButtonBicycle, R.id.mainImageButtonWalking};
    private String[] _m_modes = {"vehicle", "bikeLane", "bikeLane"};
    private String _m_mode_selected;
    private GoogleMap _m_gMap;
    private List<Lane> _m_trafficLights;
    private Location _m_location;
    private boolean _m_determinated;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _m_serviceIntent = new Intent(this, ClientService.class);
        _m_clientReceiver = new MainActivity.ClientReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SERVICE_RECEIVER);
        registerReceiver(_m_clientReceiver, intentFilter);
        Bundle req = new Bundle();
        req.putString("task", ClientService.TASK_REQ_SPAT_JSON);
        _m_serviceIntent.putExtras(req);
        startService(_m_serviceIntent);
        bindService(_m_serviceIntent, _m_serviceConnection, BIND_AUTO_CREATE);

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
                            setModeFocus(_m_imageButton_unfocus, _m_imageButtons[0]);
                            _m_mode_selected = _m_modes[0];
                            break;

                        case R.id.mainImageButtonBicycle :
                            setModeFocus(_m_imageButton_unfocus, _m_imageButtons[1]);
                            _m_mode_selected = _m_modes[1];
                            break;

                        case R.id.mainImageButtonWalking :
                            setModeFocus(_m_imageButton_unfocus, _m_imageButtons[2]);
                            _m_mode_selected = _m_modes[2];
                            break;
                    }
                }
            });
        }
        // init car imageButton selected
        _m_imageButton_unfocus = _m_imageButtons[0];
        setModeFocus(_m_imageButton_unfocus, _m_imageButtons[1]);
        _m_mode_selected = _m_modes[1];

        _s_instance = this;
        _m_determinated = false;
        _m_idDictionary = new HashMap<>();
        _m_imageResource_trafficLights = new HashMap<>();
        _m_imageResource_trafficLights.put("STOP_AND_REMAIN", R.drawable.red);
        _m_imageResource_trafficLights.put("PRE_MOVEMENT", R.drawable.red_yellow);
        _m_imageResource_trafficLights.put("PROTECTED_MOVEMENT_ALLOWED", R.drawable.green);
        _m_imageResource_trafficLights.put("PROTECTED_CLEARANCE", R.drawable.yellow);
        _m_imageResource_trafficLights.put("DARK", R.drawable.dark);

        // map, spat info
        updateSpat();
    }

    @Override
    public void onDestroy() {
        unbindService(_m_serviceConnection);
        stopService(_m_serviceIntent);
        unregisterReceiver(_m_clientReceiver);
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
                        case ClientService.RESP_REQ_SPAT_JSON:
                            if(Boolean.valueOf(value)){
                                _m_mapInfo = Utils.mapInfoParser(_m_clientBinder.getMapInfoJson());
                                _m_intersection_location = new UTMLocation(_m_mapInfo.map.intersection.positionUTM.east, _m_mapInfo.map.intersection.positionUTM.north);
                                _m_spat = Utils.spatParser(_m_clientBinder.getSpatJson());
                                _m_lanes = Utils.lanesParser(_m_clientBinder.getLanesJson());
                                _m_connections = Utils.connectionsParser(_m_clientBinder.getConnectionsJson());
                                findViewById(R.id.mainLoadingPanel).setVisibility(View.GONE);
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        _m_gMap.setMyLocationEnabled(true);
        if(_m_mapInfo != null){
            _m_trafficLights =  _m_mapInfo.map.intersection.lanes;
        }
        if(_m_trafficLights != null){
            //showTrafficLights();
        }
    }

    public static MainActivity getInstance() {
        return _s_instance;
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
    private void setModeFocus(ImageButton btn_unfocus, ImageButton btn_focus){
        btn_unfocus.setBackgroundColor(Color.rgb(232, 232, 232));
        btn_focus.setBackgroundColor(Color.rgb(181, 181, 181));
        _m_imageButton_unfocus = btn_focus;
    }


    /*
     * Input: Location object from LocationService
     * Return: none
     * Description: get current location information from LocationService,
     *              move camera of map view to current location,
     *              get street information
     *              add speed information on the map
     *              add travel trajectory on the map
     *              calculate distance to next intersection
     *              call determination function when distance is less than threshold
     */
    protected void updateLocationWGS(Location location) {
        UTMLocation currentLocation = new UTMLocation();
        currentLocation.getUTMLocationFromWGS(location.getLatitude(), location.getLongitude());

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        float speedCurrent = (float) (location.getSpeed() * 3.6); //in km/h
        if(_m_gMap != null) {
            _m_gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 19f));
            //add speed information on the current location

            //Log.i("Current Speed", String.format("%.1f km/h",speedCurrent));
            //Marker markerCurrent = _m_gMap.addMarker(new MarkerOptions()
            //        .position(locationCurrent).alpha(0.0f)
            //        .title(String.format("%.1f km/h",speedCurrent)));
            //markerCurrent.showInfoWindow();

            //add red trajectory line on the map
            if (_m_location !=null){
                Polyline line = _m_gMap.addPolyline(new PolylineOptions()
                        .add(currentLatLng, new LatLng(_m_location.getLatitude(),_m_location.getLongitude()))
                        .width(5)
                        .color(Color.RED));
            }
            _m_location=location;
        }
        if(_m_intersection_location != null){
            double distance = Utils.getUTMDistance(currentLocation, _m_intersection_location);
            if(!_m_determinated){
                if((int)distance >= 30  && (int)distance < 70){
                } else if ((int)distance < 30){
                    determinate(currentLocation);
                }
            } else {
                if ((int)distance > 30){
                    _m_determinated = false;
                }
            }
        }
    }

    /*
     * Input: none
     * Return: corresponding signal group
     * Description: do determination job
     */
    private void determinate(UTMLocation currentLocation){
        _m_determinated = true;
        int determinatedLaneId = 0;
        int determinatedSignalGroupId = 0;
        double determinatedDistance = Double.POSITIVE_INFINITY;
        for(ConnectionsFeature connection : _m_connections.features){
            double longConnectionStart = connection.geometry.coordinates.get(0).get(0);
            double latConnectionStart = connection.geometry.coordinates.get(0).get(1);
            UTMLocation connectionStart = new UTMLocation();
            connectionStart.getUTMLocationFromWGS(latConnectionStart, longConnectionStart);

            String laneType = "";
            for(LanesFeature lane : _m_lanes.features){
                if(lane.properties.laneId == Integer.valueOf(connection.properties.fromLane)){
                    laneType = lane.properties.laneType;
                }
            }
            if(laneType.equals(_m_mode_selected)){
                double distance = Utils.getUTMDistance(currentLocation, connectionStart);
                if(distance < determinatedDistance){
                    determinatedDistance = distance;
                    determinatedLaneId = Integer.valueOf(connection.properties.fromLane);
                    determinatedSignalGroupId = Integer.valueOf(connection.properties.signalGroup);
                }
            }
        }
        Log.d("Determination", "Result lane ID: "+determinatedLaneId+" signal group ID: "+determinatedSignalGroupId);
    }

    /*
     * Input: list of traffic lights states
     * Return: none
     * Description: show traffic lights with their positions and states at map view
     */
    private void showTrafficLights(){
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
    private Bitmap getTrafficLightBitmap(String state){
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
