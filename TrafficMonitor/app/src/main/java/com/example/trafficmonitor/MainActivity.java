package com.example.trafficmonitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.google.android.gms.maps.model.Marker;
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

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
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
    private CountDownTimer _m_timer;
    private boolean _m_timeCleared = false;
    private long _m_timeLeft = 0;
    private String _m_current_state;
    private int _m_current_timeLeft;

    // map, spat info parameters
    private MapInfo _m_mapInfo;
    private UTMLocation _m_intersection_location;
    private Spat _m_spat;
    private LanesKML _m_lanes;
    private ConnectionsKML _m_connections;
    private boolean _m_dataResourceLoaded = false;
    private ImageButton[] _m_imageButtons = new ImageButton[2];
    private ImageButton _m_imageButton_unfocus;
    private int[] _m_imageButton_id = {R.id.mainImageButtonCar, R.id.mainImageButtonBicycle};
    private String[] _m_modes = {"vehicle", "bikeLane"};
    private String _m_mode_selected;
    private GoogleMap _m_gMap;
    private MarkerManager _m_markerManager;
    private List<Polyline> _m_polylines;
    private List<Lane> _m_trafficLights;
    private Location _m_location;
    private boolean _m_drawPolyline = false;
    private boolean _m_autoCamera = true;
    private boolean _m_determinated = false;

    Dialog adviceDialog, settingsDialog, aboutDialog;
    ImageView closeAdvicePopupImg, closeSettingsWindowImg, closeAboutWindowImg;
    Button buttonClear;
    CheckBox drawPolylineCheckbox, autoCameraCheckbox;
    ImageButton aboutButton;

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

        settingsDialog = new Dialog(this);
        aboutDialog = new Dialog(this);
        initNotification();

        //Hide status Bar
        View overlay = findViewById(R.id.mainActivityLayout);
        overlay.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN);


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
        getSupportActionBar().hide();
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
                    }
                }
            });
        }
        // init car imageButton selected
        _m_imageButton_unfocus = _m_imageButtons[0];
        setModeFocus(_m_imageButton_unfocus, _m_imageButtons[1]);
        _m_mode_selected = _m_modes[1];
/*
        Button buttonClear = settingsDialog.findViewById(R.id.mainButtonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_m_gMap != null){
                    for(Polyline line : _m_polylines)
                    {
                        line.remove();
                    }
                    _m_polylines.clear();
                }
            }
        });*/

        /*
        CheckBox drawPolylineCheckbox = settingsDialog.findViewById(R.id.mainCheckBoxDrawPolyline);
        drawPolylineCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    _m_drawPolyline = true;
                }else{
                    _m_drawPolyline = false;
                    for(Polyline line : _m_polylines)
                    {
                        line.remove();
                    }
                    _m_polylines.clear();
                }

            }
        });
        CheckBox autoCameraCheckbox = settingsDialog.findViewById(R.id.mainCheckBoxAutoCamera);
        autoCameraCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    _m_autoCamera = true;
                }else{
                    _m_autoCamera = false;
                }

            }
        });
*/
        ImageButton setImageButton = findViewById(R.id.mainImageButtonSet);
        setImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });

        /*
        ImageButton exitSetImageButton = findViewById(R.id.mainImageButtonExitSet);
        exitSetImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //LinearLayout setGroup = findViewById(R.id.mainSetGroup);
                //setGroup.setVisibility(View.GONE);
            }
        });
         */

        _s_instance = this;
        _m_polylines = new ArrayList<>();
        _m_imageResource_trafficLights = new HashMap<>();
        _m_imageResource_trafficLights.put("STOP_AND_REMAIN", R.drawable.red);
        _m_imageResource_trafficLights.put("PRE_MOVEMENT", R.drawable.red_yellow);
        _m_imageResource_trafficLights.put("PROTECTED_MOVEMENT_ALLOWED", R.drawable.green);
        _m_imageResource_trafficLights.put("PROTECTED_CLEARANCE", R.drawable.yellow);
        _m_imageResource_trafficLights.put("DARK", R.drawable.dark);

        adviceDialog = new Dialog(this);

        aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });

    }

    public void initNotification(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(calendar.MINUTE, 43);
        calendar.set(calendar.SECOND, 30);
        Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    @Override
    protected void onDestroy() {
        unbindService(_m_serviceConnection);
        stopService(_m_serviceIntent);
        unregisterReceiver(_m_clientReceiver);
        super.onDestroy();
    }

    public void showAdvicePobup(){
        adviceDialog.setContentView(R.layout.advice_popup);
        closeAdvicePopupImg = (ImageView) adviceDialog.findViewById(R.id.closeAdvicePopup);

        closeAdvicePopupImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adviceDialog.dismiss();
            }
        });
        adviceDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        adviceDialog.show();
    }

    public void showSettings(){
        settingsDialog.setContentView(R.layout.settings_window);
        closeSettingsWindowImg = (ImageView) settingsDialog.findViewById(R.id.closeSettingsWindow);
        buttonClear = (Button) settingsDialog.findViewById(R.id.mainButtonClear);
        drawPolylineCheckbox = (CheckBox) settingsDialog.findViewById(R.id.mainCheckBoxDrawPolyline);
        autoCameraCheckbox = (CheckBox) settingsDialog.findViewById(R.id.mainCheckBoxAutoCamera);
        drawPolylineCheckbox.setChecked(_m_drawPolyline);
        autoCameraCheckbox.setChecked(_m_autoCamera);
        closeSettingsWindowImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsDialog.dismiss();
            }
        });

        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_m_gMap != null){
                    for(Polyline line : _m_polylines)
                    {
                        line.remove();
                    }
                    _m_polylines.clear();
                }
            }
        });

        drawPolylineCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    _m_drawPolyline = true;
                }else{
                    _m_drawPolyline = false;
                    for(Polyline line : _m_polylines)
                    {
                        line.remove();
                    }
                    _m_polylines.clear();
                }
            }
        });

        autoCameraCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    _m_autoCamera = true;
                }else{
                    _m_autoCamera = false;
                }

            }
        });
        settingsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        settingsDialog.show();
    }

    public void showAboutDialog(){
        aboutDialog.setContentView(R.layout.about_window);
        closeAboutWindowImg = (ImageView) aboutDialog.findViewById(R.id.closeAboutWindow);

        closeAboutWindowImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aboutDialog.dismiss();
            }
        });
        aboutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        aboutDialog.show();
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
                                _m_dataResourceLoaded = true;
                                Toast.makeText(MainActivity.this, "Get Spat Json Successfully", Toast.LENGTH_SHORT).show();
                                if(_m_mapInfo != null){
                                    _m_trafficLights =  _m_mapInfo.map.intersection.lanes;
                                }
                                if(_m_trafficLights != null){
                                    showTrafficLights();
                                }
                            } else {
                                _m_dataResourceLoaded = false;
                                Toast.makeText(MainActivity.this, "Invalid Authorization or Server down", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }
            }
        }
    }

    public static MainActivity getInstance() {
        return _s_instance;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        _m_gMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        _m_gMap.setMyLocationEnabled(true);
        View locationButton = ((View) findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(locationButton.getLayoutParams().width, locationButton.getLayoutParams().height);
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

        float dip_bottom = 100f;
        int px_bottom = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip_bottom,
                getResources().getDisplayMetrics()
        );
        float dip_right = 15f;
        int px_right = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip_right,
                getResources().getDisplayMetrics()
        );
        rlp.setMargins(0, 0, px_right, px_bottom);
        locationButton.setLayoutParams(rlp);
        _m_markerManager = new MarkerManager(_m_gMap);
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
        float currentSpeed = (float) (location.getSpeed() * 3.6); //in km/h
        if(_m_gMap != null) {
            if(_m_autoCamera){
                _m_gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 19f));
            }
            Log.i("Current Speed", String.format("%.1f km/h",currentSpeed));
            Marker markerCurrent = _m_gMap.addMarker(new MarkerOptions()
                    .position(currentLatLng).alpha(0.0f)
                    .title(String.format("%.1f km/h",currentSpeed)));
            markerCurrent.showInfoWindow();

            //add red trajectory line on the map
            if (_m_location !=null && _m_drawPolyline){
                _m_polylines.add(_m_gMap.addPolyline(new PolylineOptions()
                        .add(currentLatLng, new LatLng(_m_location.getLatitude(),_m_location.getLongitude()))
                        .width(5)
                        .color(Color.RED)));
            }
            _m_location=location;
        }
        if(_m_intersection_location != null){
            double distance = Utils.getUTMDistance(currentLocation, _m_intersection_location);
            if(!_m_determinated){
                if ((int)distance < 50){
                    if(_m_dataResourceLoaded){
                        determinate(currentLocation, currentSpeed);
                    } else {
                        Toast.makeText(MainActivity.this, "Data resource still not loaded jet", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if ((int)distance > 50){
                    _m_determinated = false;
                    adviceDialog.hide();
                }
            }
        }
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
     * Input: list of traffic lights states
     * Return: none
     * Description: show traffic lights with their positions and states at map view
     */
    private void showTrafficLights(){
        if (_m_markerManager!=null){
            MarkerManager.Collection markerCollection = _m_markerManager.newCollection("TL");
            for(int i = 0; i < _m_trafficLights.size(); i++){
                Marker marker = markerCollection.addMarker(
                        new MarkerOptions()
                                .position(
                                        new LatLng(
                                                _m_trafficLights.get(i).positionWGS84.lat,
                                                _m_trafficLights.get(i).positionWGS84.lng
                                        )
                                )
                                .icon(BitmapDescriptorFactory.fromBitmap(getTrafficLightBitmap("STOP_AND_REMAIN")))
                                .title("Lane: " + _m_trafficLights.get(i).id + " Signal Group:" +_m_trafficLights.get(i).signalGroup)
                );
                marker.setTag(_m_trafficLights.get(i).signalGroup);
                timerTLOnMap(_m_trafficLights.get(i).signalGroup);
            }
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

            case "STOP_AND_REMAIN":
                is = getResources().openRawResource(R.drawable.red);
                break;
            case "DARK":
                is = getResources().openRawResource(R.drawable.dark);
                break;
            case "PRE_MOVEMENT":
                is = getResources().openRawResource(R.drawable.red_yellow);
                break;
            case "PROTECTED_MOVEMENT_ALLOWED":
                is = getResources().openRawResource(R.drawable.green);
                break;
            case "PROTECTED_CLEARANCE":
                is = getResources().openRawResource(R.drawable.yellow);
                break;
            default:
                is = getResources().openRawResource(R.drawable.red);
                break;
        }
        Bitmap bm = BitmapFactory.decodeStream(is);
        // before return, bitmap was scaled
        return Bitmap.createScaledBitmap(bm, bm.getWidth()/2, bm.getHeight()/2,true);
    }

    /*
     * Input: none
     * Return: corresponding signal group
     * Description: do determination job
     *              currentSpeed in km/h
     */
    private void determinate(UTMLocation currentLocation, float currentSpeed){
        _m_determinated = true;
        if(_m_timer != null){
            _m_timeCleared = true;
            _m_timer.cancel();
        }
        int determinatedLaneId = 0;
        int determinatedSignalGroupId = 0;
        List<String> determinatedManeuvers = new ArrayList();
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
                double distance = Utils.getUTMDistance(currentLocation, connectionStart); // meter
                if(distance < determinatedDistance){
                    determinatedDistance = distance;
                    determinatedLaneId = Integer.valueOf(connection.properties.fromLane);
                    determinatedSignalGroupId = Integer.valueOf(connection.properties.signalGroup);
                    if(!determinatedManeuvers.contains(connection.properties.maneuver)){
                        determinatedManeuvers.add(connection.properties.maneuver);
                    }
                } else if (distance == determinatedDistance){
                    if(!determinatedManeuvers.contains(connection.properties.maneuver)){
                        determinatedManeuvers.add(connection.properties.maneuver);
                    }
                }
            }
        }
        Log.d("Determination", "Result lane ID: "+determinatedLaneId+" signal group ID: "+determinatedSignalGroupId+" maneuvers: "+determinatedManeuvers.size());

        String maneuvers = "";
        for(String maneuver : determinatedManeuvers){
            switch (maneuver){
                case "maneuverRightAllowed":
                    maneuvers += "Right ";
                    break;
                case "maneuverStraightAllowed":
                    maneuvers += "Straight ";
                    break;
                case "maneuverLeftAllowed":
                    maneuvers += "Left ";
                    break;
            }
        }

        showAdvicePobup();
        generateAdvice(currentSpeed, determinatedDistance, determinatedSignalGroupId);

    }

    /*
     * Input: none
     * Return: none
     * Description: set corresponding traffic light model and generate driving advice
     *              currentSpeed in km/h
     */
    private void generateAdvice(float currentSpeed, double determinatedDistance, int determinatedSignalGroupId){
        timer(determinatedSignalGroupId);
        TextView textViewAdvice = adviceDialog.findViewById(R.id.mainTextViewAdvice);
        TextView textViewAdviceSpeed = adviceDialog.findViewById(R.id.mainTextViewAdviceSpeed);
        //set different upper&lower speed limit
        double upperLimit = 0.0;
        double lowerLimit = 0.0;
        if (_m_mode_selected.equals("vehicle")){
            upperLimit = 50.0; // 50km/h
            lowerLimit = 30.0; // 30 km/h
        }
        else if (_m_mode_selected.equals("bikeLane")){
            upperLimit = 15.0; // 15 km/h
            lowerLimit = 0.0; // 0 km/h
        }

        //store duration of each state phase
        HashMap<String,Integer> durationOfEachState= new HashMap<>();
        String firstState = _m_spat.intersectionStates.get(0).movementStates.get(determinatedSignalGroupId-1).movementEvents.get(0).phaseState;
        int lastStateLikelyTime = _m_spat.intersectionStates.get(0).movementStates.get(determinatedSignalGroupId-1).movementEvents.get(0).timeChange.likelyTime / 10;
        for(int j = 1; j < _m_spat.intersectionStates.get(0).movementStates.get(determinatedSignalGroupId-1).movementEvents.size(); j++){
            MovementEvent movementEvent = _m_spat.intersectionStates.get(0).movementStates.get(determinatedSignalGroupId-1).movementEvents.get(j);
            int likelyTime = movementEvent.timeChange.likelyTime/10;
            if(movementEvent.phaseState.equals(firstState)){
                durationOfEachState.put(movementEvent.phaseState,likelyTime-lastStateLikelyTime);
                break;
            }
            durationOfEachState.put(movementEvent.phaseState,likelyTime-lastStateLikelyTime);
            lastStateLikelyTime=likelyTime;
        }

        // Strategies in different situations
        double adviceSpeed;
        int timeLeft=0;

        if (currentSpeed>upperLimit){
            textViewAdvice.setText("Too fast！Slow down!");
            textViewAdviceSpeed.setText(String.format("%.2f",upperLimit));
        }else{
            if ((_m_current_state.equals("PROTECTED_MOVEMENT_ALLOWED"))||(_m_current_state.equals("PROTECTED_CLEARANCE"))){
                if (_m_current_state.equals("PROTECTED_MOVEMENT_ALLOWED")){
                    timeLeft = _m_current_timeLeft+durationOfEachState.get("PROTECTED_CLEARANCE");
                }else{
                    timeLeft = _m_current_timeLeft;
                }
                adviceSpeed = determinatedDistance/timeLeft*3.6; //in km/h
                if(adviceSpeed > upperLimit){
                    //only a very little time left, not possible to reach
                    textViewAdvice.setText("Stop! Out of time!");
                    textViewAdviceSpeed.setText("0");
                } else if(adviceSpeed > currentSpeed){
                    //still enough time to reach, but need to speed up
                    textViewAdvice.setText("Speed Up! GOGOGO!");
                    textViewAdviceSpeed.setText(String.format("%.2f",adviceSpeed));
                } else{
                    //very enough time, and only little distance
                    textViewAdvice.setText("Keep your speed! Time is enough!");
                    textViewAdviceSpeed.setText(String.format("%.2f",currentSpeed));
                }

            } else if ((_m_current_state.equals("STOP_AND_REMAIN"))||(_m_current_state.equals("PRE_MOVEMENT"))){
                if (_m_current_state.equals("STOP_AND_REMAIN")){
                    timeLeft=_m_current_timeLeft+durationOfEachState.get("PRE_MOVEMENT");
                }else{
                    timeLeft = _m_current_timeLeft;
                }
                adviceSpeed = determinatedDistance/timeLeft*3.6; // in km/h
                if(adviceSpeed < lowerLimit){
                    //still a long time to wait for green light
                    textViewAdvice.setText("Stop! Still a long time before green!");
                    textViewAdviceSpeed.setText("0");
                } else if(adviceSpeed > upperLimit){
                    //The light will soon be green
                    textViewAdvice.setText("Keep your speed! Green light is coming!");
                    textViewAdviceSpeed.setText(String.format("%.2f",currentSpeed));
                } else{
                    if(adviceSpeed < currentSpeed){
                        //still a long time to wait for green light, but not too long
                        textViewAdvice.setText("Slow down! Still time before green!");
                        textViewAdviceSpeed.setText(String.format("%.2f",adviceSpeed));
                    } else if(adviceSpeed > currentSpeed){
                        //The light will soon be green, but no too soon
                        textViewAdvice.setText("Keep your speed! Green light is coming!");
                        textViewAdviceSpeed.setText(String.format("%.2f",adviceSpeed));
                    }else if(adviceSpeed == currentSpeed){
                        //The light will soon be green, but no too soon
                        textViewAdvice.setText("Keep your speed! Green light is coming!");
                        textViewAdviceSpeed.setText(String.format("%.2f",adviceSpeed));
                    }
                }
            }
        }
    }

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
        _m_current_state=state;
        timeLeft = Long.valueOf(result[1]);
        _m_timeLeft = timeLeft;
        //updateTrafficLight
        //ImageView imageView = (ImageView) findViewById(_m_idDictionary.get("trafficLight"+signalGroupId));
        ImageView imageView = (ImageView) adviceDialog.findViewById(R.id.mainImageViewTrafficLight);
        if(_m_imageResource_trafficLights.get(state) != null)
        {
            imageView.setImageResource(_m_imageResource_trafficLights.get(state));
        }
        _m_timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                //updateCountDownText
                int seconds = (int) millisUntilFinished / 1000;
                _m_current_timeLeft = seconds;
                String timeLeftFormatted = String.format(Locale.getDefault(), "%02d", seconds);
                TextView textViewTimer = (TextView) adviceDialog.findViewById(R.id.mainTextViewTimer);
                textViewTimer.setText(timeLeftFormatted);
            }
            @Override
            public void onFinish() {
                if(!_m_timeCleared){
                    timer(signalGroupId);
                } else {
                    Log.d(TAG, "Timer cleared");
                    _m_timeCleared = false;
                }
            }
        };
        _m_timer.start();
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
     * Description: start timer and update ImageView of traffic lights on the map
     */
    private void timerTLOnMap(int signalGroupId) {
        long timeLeft;
        String state;
        String[] result = calculateCurrentState(signalGroupId).split(":");
        state = result[0];
        timeLeft = Long.valueOf(result[1]);
        //updateTrafficLight

        for(Object obj :_m_markerManager.getCollection("TL").getMarkers().toArray()){
            Marker marker = (Marker) obj;
            if((Integer)marker.getTag() == signalGroupId){
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getTrafficLightBitmap(state)));
            }
        }
        new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                //updateCountDownText
                int seconds = (int) millisUntilFinished / 1000;
                String timeLeftFormatted = String.format(Locale.getDefault(), "%02d", seconds);
            }
            @Override
            public void onFinish() {
                if(!_m_timeCleared){
                    timerTLOnMap(signalGroupId);
                } else {
                    Log.d(TAG, "Timer cleared");
                    _m_timeCleared = false;
                }
            }
        }.start();
    }

}
