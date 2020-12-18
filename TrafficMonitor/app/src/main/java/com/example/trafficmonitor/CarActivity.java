package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;

import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CarActivity extends AppCompatActivity {

    // Traffic light timer parameters
    private TextView _m_textViewCountDown;
    private Button _m_btnStartSimulation;
    private CountDownTimer _m_countDownTimer;
    private long _m_timeLeftInMillis = 6000;
    private ImageView _m_imageView_trafficLight;
    private int _m_current_image_trafficLight;
    private int[] _m_images_trafficLight = {R.drawable.red, R.drawable.red_yellow, R.drawable.yellow, R.drawable.green};

    // GPS info parameters
    private static CarActivity _m_instance;
    private TextView _m_textView_gpsInfo;
    private UTMLocation _m_current_utm_location;
    private float _m_current_speed; //in m/s

    // map, spat info parameters
    private int _m_timeLeft_cal_neceSpeed = 25; // second
    private TextView _m_textView_mapInfo;
    private MapInfo _m_mapInfo;
    private UTMLocation _m_intersection_location;
    private TextView _m_textView_spat;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Car Activity");
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        _m_textViewCountDown = findViewById(R.id.carTextViewTimer1);
        _m_btnStartSimulation = findViewById(R.id.carButtonTest);
        _m_textView_gpsInfo = findViewById(R.id.carTextViewGPS);
        _m_textView_mapInfo = findViewById(R.id.carTextViewMapInfo);
        _m_textView_spat = findViewById(R.id.carTextViewTrafficLightState);
        _m_btnStartSimulation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer();
            }
        });

        //Traffic light timer
        updateCountDownText();

        // GPS info
        _m_instance = this;

        // map, spat info
        updateMapInfoText();
        updateSpatText();
    }

    // Traffic light timer functions
    /*
     * Input: none
     * Return: none
     * Description: start timer and update TextView, ImageView of traffic lights
     */
    private void timer() {
        _m_countDownTimer = new CountDownTimer(_m_timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                _m_timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }
            @Override
            public void onFinish() {
                _m_timeLeftInMillis = 6000;
                updateCountDownText();
                updateTrafficLight();
            }
        }.start();
    }

    /*
     * Input: none
     * Return: none
     * Description: update TextView of traffic lights
     */
    private void updateCountDownText() {
        int seconds = (int) (_m_timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d", seconds);
        _m_textViewCountDown.setText(timeLeftFormatted);
    }

    /*
     * Input: none
     * Return: none
     * Description: update ImageView of traffic lights
     */
    public void updateTrafficLight() {
        _m_imageView_trafficLight = (ImageView) findViewById(R.id.carImageViewTrafficLight1);
        _m_current_image_trafficLight++;
        _m_current_image_trafficLight = _m_current_image_trafficLight % _m_images_trafficLight.length;
        _m_imageView_trafficLight.setImageResource(_m_images_trafficLight[_m_current_image_trafficLight]);
    }

    // GPS info functions

    /*
     * Input: none
     * Return: none
     * Description: get instance of CarActivity, called by LocationService
     */
    public static CarActivity getInstance() {
        return _m_instance;
    }

    /*
     * Input: UTMLocation object
     * Return: none
     * Description: update UTMLocation, called by LocationService
     */
    public void updateUtmLocation(UTMLocation location) {
        _m_current_utm_location = location;
    }

    /*
     * Input: float number of speed
     * Return: none
     * Description: update speed, called by LocationService
     */
    public void updateSpeed(float speed) {
        _m_current_speed = speed;
    }

    /*
     * Input: String of GPS and speed information
     * Return: none
     * Description: update GPS TextView, called by LocationService
     */
    public void updateGPSTextView(String value) {
        CarActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _m_textView_gpsInfo.setText(value);
            }
        });
    }

    /*
     * Input: UTMLocation object
     * Return: none
     * Description: update distance to intersection and calculate necessary speed, called by LocationService
     *              Route from "Scharnweberstraße 132, 13405 Berlin" to "Scharnweberstraße 140, 13405 Berlin"
     *              with Speed 1X almost 4 m/s used to test
     */
    public void updateDistanceToIntersection(UTMLocation curretnLocation){
        if(_m_intersection_location != null){
            double distance = Utils.getUTMDistance(curretnLocation, _m_intersection_location);
            Log.i("Distance",distance+" m");
            double speed_nec = distance / _m_timeLeft_cal_neceSpeed; // m/s
            Log.i("Necessary Speed",speed_nec+" m/s -> "+speed_nec*3.6+"km/h");
        }
    }


    // map, spat info functions
    /*
     * Input: none
     * Return: none
     * Description: get json of mapInfo from server, parse it into object and update MapInfo TextView
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateMapInfoText() {
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
    }

    /*
     * Input: none
     * Return: none
     * Description: get json of spat from server, parse it into object and update spat TextView with
     *              state and left Time of first signal group
     */
    public void updateSpatText() {
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CurrentState currentState = phasesLastSignalGroup.getCurrentState();
                            currentState.setPostionWGS(_m_mapInfo.map.intersection.lanes);
                            List<CurrentState> trafficLights = new ArrayList<>();
                            trafficLights.add(currentState);
                            _m_textView_spat.setText("Signal Group: " + currentState.signalGroupId + " "
                                    + currentState.state + " Left: " + currentState.timeLefts + " s");
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
}


