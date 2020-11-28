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

    /*Http Client and Map Info JSON Parser (Yuanheng)*/
    //lste[length-1]
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void clientMapInfoTest() {
        Runnable runnable = new Runnable(){
            public void run() {
                try {
                    MapInfo mapInfo = Utils.mapInfoParser(Utils.getMapInfoJson());
                    Log.i("Client MapInfo", String.valueOf(mapInfo.map.intersection.intersectionID));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }
    private static TextView m_JSONObject;
    /*Http Client and Spat JSON Parser (Yuanheng)*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void clientSpatTest() {
        Runnable runnable = new Runnable(){
            public void run() {
                try {
                    Spat spat = Utils.spatParser(Utils.getSpatJson()); // need almost 12 sec to get json from api
                    Log.i("Client Spat", String.valueOf(spat.intersectionStates.get(0).movementStates.size()));
                } catch (IOException e) {
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
        m_JSONObject = findViewById(R.id.textView9);

        m_btnStartSimulation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer();
            }
        });
        updateCountDownText();
        //clientMapInfoTest();
        //clientSpatTest();
    }

}