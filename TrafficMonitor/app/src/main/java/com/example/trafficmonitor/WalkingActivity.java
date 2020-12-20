package com.example.trafficmonitor;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class WalkingActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static WalkingActivity _m_instance;
    private GoogleMap _m_gMap;
    private Location _m_location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walking);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Walking Activity");
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        _m_instance = this;
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
    }

    public static WalkingActivity getInstance() {
        return _m_instance;
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
            Marker markerCurrent = _m_gMap.addMarker(new MarkerOptions()
                    .position(locationCurrent).alpha(0.0f)
                    .title(String.format("%.1f km/h",speedCurrent)));
            markerCurrent.showInfoWindow();
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
     * Input: list of traffic lights states
     * Return: none
     * Description: show traffic lights with their positions and states at map view
     */
    public void showTrafficLights(List<CurrentState> trafficLights){
        MarkerManager markerManager = new MarkerManager(_m_gMap);
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
}