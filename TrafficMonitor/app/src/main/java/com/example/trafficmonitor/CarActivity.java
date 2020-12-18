package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.maps.android.collections.MarkerManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CarActivity extends AppCompatActivity implements OnMapReadyCallback {

    /*Timer Functions*/
    private TextView m_textViewCountDown;
    private Button m_btnStartSimulation;
    private CountDownTimer m_countDownTimer;
    private long m_timeLeftInMillis = 6000;

    private void timer() {
        m_countDownTimer = new CountDownTimer(m_timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                m_timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                m_timeLeftInMillis = 6000;
                updateCountDownText();
                updateTrafficLight();
            }
        }.start();
    }

    private void updateCountDownText() {
        int seconds = (int) (m_timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d", seconds);
        m_textViewCountDown.setText(timeLeftFormatted);
    }
    /*Timer Functions*/

    /*Traffic Light Functions*/
    private static ImageView m_imageView;
    private int current_image;
    int[] images = {R.drawable.red, R.drawable.red_yellow, R.drawable.yellow, R.drawable.green};

    public void updateTrafficLight() {
        m_imageView = (ImageView) findViewById(R.id.imageView3);
        current_image++;
        current_image = current_image % images.length;
        m_imageView.setImageResource(images[current_image]);
    }
    /*Traffic Light Functions*/

    /*GPS Information (Yiyang)*/

    private static TextView m_gpsInformation;
    static CarActivity instance;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    double utmLocationEasting;  // in meters
    double utmLocationNorthing; // in meters
    float currentSpeed; //in m/s

    public static CarActivity getInstance() {
        return instance;
    }


    private void updateLocation() {
        buildLocationRequest();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());

    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationService.class);
        intent.setAction(LocationService.ACTION_PROCESS_UPDATE);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //set the time interval and distance interval of request
    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000); // in milliseconds (1ms = 0.001s)
        locationRequest.setFastestInterval(1000); // in milliseconds (1ms = 0.001s)
        locationRequest.setSmallestDisplacement(0.5f); // in meters
    }


    //update current UTM Location Information for further use
    public void updateUtmLocation(double easting, double northing) {
        this.utmLocationEasting = easting;
        this.utmLocationNorthing = northing;
    }

    //update current speed Information for further use
    public void updateSpeedInfo(float speed) {
        this.currentSpeed = speed;
    }

    public void updateLocationWGS(Location location) {
        LatLng locationCurrent = new LatLng(location.getLatitude(), location.getLongitude());
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationCurrent, 19f));
    }


    /*only used for Textview for function testing  */
    public void updateTextView(String value) {
        CarActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_gpsInformation.setText(value);
            }
        });
    }
    /*only used for Textview for function testing  */
    /*Distance and necessary speed calculation(Yuanheng)*/
    // Route from "Scharnweberstraße 132, 13405 Berlin" to "Scharnweberstraße 140, 13405 Berlin"
    // with Speed 1X almost 4 m/s used to test
    int timeLeft = 25; // second
    public void updateDistanceToIntersection(UTMLocation curretnLocation, float speed){
        if(intersectionLocation != null){
            double distance = Utils.getUTMDistance(curretnLocation, intersectionLocation);
            //Log.i("Distance",distance+" m");
            double speed_nec = distance / timeLeft; // m/s
            //Log.i("Necessary Speed",speed_nec+" m/s -> "+speed_nec*3.6+"km/h");
        }
    }

    /*GPS Information*/

    /*Http Client and JSON Parser of Map Info(Yuanheng)*/
    private static TextView m_MapInfo;
    private MapInfo mapInfo;
    UTMLocation intersectionLocation;
    // Async processing
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateMapInfoText() {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    String mapInfoStr = Utils.getMapInfoJson(getResources().getString(R.string.map_info_url));
                    mapInfo = Utils.mapInfoParser(mapInfoStr);
                    int intersectionID = mapInfo.map.intersection.intersectionID;
                    intersectionLocation = new UTMLocation(mapInfo.map.intersection.positionUTM.east,mapInfo.map.intersection.positionUTM.north);
                    //Update view at main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            m_MapInfo.setText("Intersection ID: " + String.valueOf(intersectionID));
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
    }

    /*Http Client and JSON Parser of Spat(Yuanheng)*/
    // State and left Time of last signal group

    private static TextView m_Spat;
    //Update view at main thread
    public void updateSpatText(Phases phasesLastSignalGroup) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CurrentState currentState = phasesLastSignalGroup.getCurrentState();
                currentState.setPostionWGS(mapInfo.map.intersection.lanes);
                List<CurrentState> trafficLights = new ArrayList<>();
                trafficLights.add(currentState);
                getTrafficLights(trafficLights);
                m_Spat.setText("Signal Group: " + currentState.signalGroupId + " "
                        + currentState.state + " Left: " + currentState.timeLefts + " s");
            }
        });
    }

    // Async processing
    public void requestSpat() {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
                try {
                    String spatStr = Utils.getSpatJson(getResources().getString(R.string.spat_url)); // need almost 12s
                    Spat spat = Utils.spatParser(spatStr);
                    List<MovementState> movementStates = spat.intersectionStates.get(0).movementStates;
                    Long timestamp = spat.timestamp;
                    int signalGroupId = movementStates.get(0).signalGroupId;
                    List<MovementEvent> movementEvents = movementStates.get(0).movementEvents;
                    Phases phasesLastSignalGroup = new Phases(timestamp, signalGroupId, movementEvents);
                    updateSpatText(phasesLastSignalGroup);
                } catch (Exception e) {
                    Log.w("Client", "Invalid Authorization or Server down. Please check AuthUrlInfo");
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        m_textViewCountDown = findViewById(R.id.textView5);
        m_btnStartSimulation = findViewById(R.id.button);
        m_gpsInformation = findViewById(R.id.textView8);
        m_MapInfo = findViewById(R.id.textView9);
        m_Spat = findViewById(R.id.textView10);
        m_btnStartSimulation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer();
            }
        });
        updateCountDownText();

        /*by Yuanheng*/
        updateMapInfoText();
        requestSpat();

        /*by Yiyang*/
        instance = this;
        //ask permission for GPS Information request
        Dexter.withContext(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(CarActivity.this, "You need to accept Location request", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
    }
    private GoogleMap gMap;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        //LatLng intersection = new LatLng(52.564232999999994, 13.327774999999999);
        //gMap.addMarker(new MarkerOptions().position(intersection).title("Reference Point of Intersection 14052"));
        //gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(intersection, 19f));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        gMap.setMyLocationEnabled(true);
    }
    public void getTrafficLights(List<CurrentState> trafficLights){
        MarkerManager markerManager = new MarkerManager(gMap);
        MarkerManager.Collection markerCollection = markerManager.newCollection();
        //Log.i("test",String.valueOf(trafficLights.size()));
        for(int i = 0; i < trafficLights.size(); i++){
            markerCollection.addMarker(
                    new MarkerOptions()
                            .position(
                                    new LatLng(
                                            trafficLights.get(i).postionWGS.lat,
                                            trafficLights.get(i).postionWGS.lng
                                    )
                            )
                            .icon(BitmapDescriptorFactory.fromBitmap(getTrafficLightBitmap("STOP_AND_REMAIN")))
                            .title("nameTest")
            );
        }
    }
    @SuppressLint("ResourceType")
    public Bitmap getTrafficLightBitmap(String state){
        InputStream is;
        switch (state){
            default:
                is = getResources().openRawResource(R.drawable.red);
                break;
        }
        Bitmap bm = BitmapFactory.decodeStream(is);
        return Bitmap.createScaledBitmap(bm, bm.getWidth()/2, bm.getHeight()/2,true);
    }
}


