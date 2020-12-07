package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {

    /*Timer Functions*/
    private TextView m_textViewCountDown;
    private Button m_btnStartSimulation;
    private CountDownTimer m_countDownTimer;
    private long m_timeLeftInMillis = 6000;

    private void timer(){
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
    private void updateCountDownText(){
        int seconds = (int) (m_timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(),"%02d", seconds);
        m_textViewCountDown.setText(timeLeftFormatted);
    }
    /*Timer Functions*/

    /*Traffic Light Functions*/
    private static ImageView m_imageView;
    private int current_image;
    int[] images = {R.drawable.red, R.drawable.red_yellow, R.drawable.yellow, R.drawable.green};

    public void updateTrafficLight(){
        m_imageView = (ImageView)findViewById(R.id.imageView3);
        current_image++;
        current_image=current_image % images.length;
        m_imageView.setImageResource(images[current_image]);
    }
    /*Traffic Light Functions*/

    /*GPS Information (Yiyang)*/
    private static TextView m_gpsInformation;


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
                            m_MapInfo.setText("Intersevtion ID: " + String.valueOf(intersectionID));
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
    //list[0]
    private static TextView m_Spat;
    @RequiresApi(api = Build.VERSION_CODES.O)
    // Async processing
    public void updateSpatText() {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    String spatStr = Utils.getSpatJson();
                    Spat spat = Utils.spatParser(spatStr);
                    List<MovementState> movementStates = spat.intersectionStates.get(0).movementStates;
                    String lastGroupState = movementStates.get(movementStates.size()-1).movementEvents.get(1).phaseState;
                    //Update view at main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            m_Spat.setText("Last Group: " + lastGroupState);
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
        updateMapInfoText();
        updateSpatText();
    }

}