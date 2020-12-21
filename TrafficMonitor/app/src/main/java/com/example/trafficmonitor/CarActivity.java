package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;

import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
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
    private int[] _m_images_trafficLight = {R.drawable.red, R.drawable.red_yellow, R.drawable.green,R.drawable.yellow};
    private String[] _m_state_trafficLight = {"STOP_AND_REMAIN","PRE_MOVEMENT","PROTECTED_MOVEMENT_ALLOWED","PROTECTED_CLEARANCE"};
    private int[] _m_time_trafficLight = {0,0,0,0};
    private static Context _m_context;
    private HashMap<String, Integer> _m_idDictionary;

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
        _m_context = this.getApplicationContext();
        _m_idDictionary = new HashMap<String, Integer>();
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
                //timer();
            }
        });

        //Traffic light timer
        //updateCountDownText();

        // GPS info
        _m_instance = this;

        // map, spat info
        updateMapInfoText();
        updateSpatText();
    }

    /*
     * Input: none
     * Return: Context object
     * Description: get context of CarActivity
     */
    public static Context getContext() {
        return _m_context;
    }

    // Traffic light timer functions
    /*
     * Input: none
     * Return: none
     * Description: start timer and update TextView, ImageView of traffic lights
     */
    private void timer(String state, int timeLefts) {
        switch (state) {
            case "STOP_AND_REMAIN":
                _m_current_image_trafficLight = 0;
                break;
            case "PRE_MOVEMENT":
                _m_current_image_trafficLight = 1;
                break;
            case "PROTECTED_MOVEMENT_ALLOWED":
                _m_current_image_trafficLight = 2;
                break;
            case "PROTECTED_CLEARANCE":
                _m_current_image_trafficLight = 3;
                break;
        }
        updateTrafficLight();
        _m_timeLeftInMillis = timeLefts*1000;
        _m_countDownTimer = new CountDownTimer(_m_timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                _m_timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }
            @Override
            public void onFinish() {
                //_m_timeLeftInMillis = 6000;
                _m_current_image_trafficLight++;
                _m_current_image_trafficLight = _m_current_image_trafficLight % _m_images_trafficLight.length;
                timer(_m_state_trafficLight[_m_current_image_trafficLight],_m_time_trafficLight[_m_current_image_trafficLight]);
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
        TextView textView = (TextView) findViewById(_m_idDictionary.get("timer1"));
        textView.setText(timeLeftFormatted);
        //_m_textViewCountDown.setText(timeLeftFormatted);
    }

    /*
     * Input: none
     * Return: none
     * Description: update ImageView of traffic lights
     */
    public void updateTrafficLight() {
        ImageView imageView = (ImageView) findViewById(_m_idDictionary.get("trafficLight1"));
        //_m_imageView_trafficLight = (ImageView) findViewById(R.id.carImageViewTrafficLight1);
        //_m_imageView_trafficLight.setImageResource(_m_images_trafficLight[_m_current_image_trafficLight]);
        imageView.setImageResource(_m_images_trafficLight[_m_current_image_trafficLight]);
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
     * Description: get json of spat from server,
     *              parse it into object,
     *              add ImageViews of traffic light
     *              update spat TextView with state and left Time of first signal group
     */
    public void updateSpatText() {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
                try {
                    String spatStr = Utils.getSpatJson(getResources().getString(R.string.spat_url)); // need almost 12s
                    Spat spat = Utils.spatParser(spatStr);
                    List<MovementState> movementStates = spat.intersectionStates.get(0).movementStates;
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
                                    String idTrafficLight = String.valueOf(System.currentTimeMillis()).substring(4);
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
                                    String idTimer = String.valueOf(System.currentTimeMillis()).substring(4);
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
                    Long timestamp = spat.timestamp;
                    int signalGroupId = movementStates.get(0).signalGroupId;
                    List<MovementEvent> movementEvents = movementStates.get(0).movementEvents;
                    Phases phasesFirstSignalGroup = new Phases(timestamp, signalGroupId, movementEvents);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CurrentState currentState = phasesFirstSignalGroup.getCurrentState();
                            currentState.setPostionWGS(_m_mapInfo.map.intersection.lanes);
                            // add time of trafficlight state
                            HashMap<String, Integer> statesTimeLeft = phasesFirstSignalGroup.getStatesTimeLeft();
                            _m_time_trafficLight[0] = statesTimeLeft.get("STOP_AND_REMAIN");
                            _m_time_trafficLight[1] = statesTimeLeft.get("PRE_MOVEMENT");
                            _m_time_trafficLight[2] = statesTimeLeft.get("PROTECTED_MOVEMENT_ALLOWED");
                            _m_time_trafficLight[3] = statesTimeLeft.get("PROTECTED_CLEARANCE");
                            timer(currentState.state, currentState.timeLefts);
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


