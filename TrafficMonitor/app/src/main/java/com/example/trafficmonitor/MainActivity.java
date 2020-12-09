package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
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
    static MainActivity instance;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;

    public static MainActivity getInstance() {
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

    public void updateTextView(String value){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_gpsInformation.setText(value);
            }
        });
    }


    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this,LocationService.class);
        intent.setAction(LocationService.ACTION_PROCESS_UPDATE);
        return PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setSmallestDisplacement(0);
    }

    /*GPS Information*/

    /*Http Client and JSON Parser of Map Info(Yuanheng)*/
    private static TextView m_MapInfo;
    // Async processing
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateMapInfoText() {
        Runnable runnable = new Runnable(){
            public void run() {
                try {
                    String mapInfoStr = Utils.getMapInfoJson();
                    MapInfo mapInfo = Utils.mapInfoParser(mapInfoStr);
                    int intersectionID = mapInfo.map.intersection.intersectionID;
                    //Update view at main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            m_MapInfo.setText("Intersection ID: " + String.valueOf(intersectionID));
                        }
                    });
                } catch (Exception e) {
                    Log.w("Client","Invalid Authorization or Server down. Please check AuthUrlInfo");
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
                m_Spat.setText("Signal Group: "+currentState.signalGroupId+" "
                        +currentState.state+" Left: "+currentState.timeLefts+" s");
            }
        });
    }
    // Async processing
    public void requestSpat() {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
                try {
                    String spatStr = Utils.getSpatJson(); // need almost 12s
                    Spat spat = Utils.spatParser(spatStr);
                    List<MovementState> movementStates = spat.intersectionStates.get(0).movementStates;
                    Long timestamp = spat.timestamp;
                    int signalGroupId = movementStates.get(0).signalGroupId;
                    List<MovementEvent> movementEvents = movementStates.get(0).movementEvents;
                    Phases phasesLastSignalGroup = new Phases(timestamp,signalGroupId,movementEvents);
                    updateSpatText(phasesLastSignalGroup);
                } catch (Exception e) {
                    Log.w("Client","Invalid Authorization or Server down. Please check AuthUrlInfo");
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
        setContentView(R.layout.activity_main);

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
                        Toast.makeText(MainActivity.this, "You need to accept Location request", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
    }



}