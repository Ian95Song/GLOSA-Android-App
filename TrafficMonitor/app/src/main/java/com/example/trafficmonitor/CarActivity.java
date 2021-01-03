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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CarActivity extends AppCompatActivity {

    // Traffic light timer parameters
    HashMap<String, Integer> _m_imageResource_trafficLights;
    private static Context _m_context;
    private HashMap<String, Integer> _m_idDictionary;
    private List<String> _m_currentState_trafficLights;


    // GPS info parameters
    private static CarActivity _m_instance;
    private TextView _m_textView_gpsInfo;
    private UTMLocation _m_current_utm_location;
    private float _m_current_speed; //in m/s

    // map, spat info parameters
    private int _m_timeLeft_cal_neceSpeed = 25; // second
    private TextView _m_textView_mapInfo;
    private MapInfo _m_mapInfo;
    private Spat _m_spat;
    private UTMLocation _m_intersection_location;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);
        _m_context = this.getApplicationContext();
        _m_idDictionary = new HashMap<>();
        _m_imageResource_trafficLights = new HashMap<>();
        _m_imageResource_trafficLights.put("STOP_AND_REMAIN", R.drawable.red);
        _m_imageResource_trafficLights.put("PRE_MOVEMENT", R.drawable.red_yellow);
        _m_imageResource_trafficLights.put("PROTECTED_MOVEMENT_ALLOWED", R.drawable.green);
        _m_imageResource_trafficLights.put("PROTECTED_CLEARANCE", R.drawable.yellow);
        _m_imageResource_trafficLights.put("DARK", R.drawable.dark);
        _m_currentState_trafficLights = new ArrayList<>();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Traffic Light Monitor");
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        _m_textView_gpsInfo = findViewById(R.id.carTextViewGPS);
        _m_textView_mapInfo = findViewById(R.id.carTextViewMapInfo);

        // GPS info
        _m_instance = this;

        // map, spat info
        updateMapInfoText();
        updateSpat();
    }

    /*
     * Input: none
     * Return: none
     * Description: get instance of CarActivity, called by LocationService
     */
    public static CarActivity getInstance() {
        return _m_instance;
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
    private void updateSpat() {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
                try {
                    String spatStr = Utils.getSpatJson(getResources().getString(R.string.spat_url)); // need almost 12s
                    _m_spat = Utils.spatParser(spatStr);
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


