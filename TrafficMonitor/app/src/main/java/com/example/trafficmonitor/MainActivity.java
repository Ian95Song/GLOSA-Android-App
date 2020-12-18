package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {
    private LocationRequest _m_locationRequest;
    private FusedLocationProviderClient _m_fusedLocationProviderClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton changeToCarActivity = (ImageButton) findViewById(R.id.mainImageButtonCar);
        ImageButton changeToBicycleActivity = (ImageButton) findViewById(R.id.mainImageButtonBicycle);
        ImageButton changeToWalkingActivity = (ImageButton) findViewById(R.id.mainImageButtonWalking);
        changeToCarActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCarActivity();
            }
        });
        changeToBicycleActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBicycleActivity();
            }
        });
        changeToWalkingActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWalkingActivity();
            }
        });
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
    }
    public void openCarActivity(){
        Intent carIntent = new Intent(MainActivity.this, CarActivity.class);
        startActivity(carIntent);
        //finish();
    }
    public void openBicycleActivity(){
        Intent bicycleIntent = new Intent(MainActivity.this, BicycleActivity.class);
        startActivity(bicycleIntent);
        //finish();
    }
    public void openWalkingActivity(){
        Intent walkingIntent = new Intent(MainActivity.this, WalkingActivity.class);
        startActivity(walkingIntent);
        //finish();
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
        Intent intent = new Intent(this, LocationService.class);
        intent.setAction(LocationService.ACTION_PROCESS_UPDATE);
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

}
