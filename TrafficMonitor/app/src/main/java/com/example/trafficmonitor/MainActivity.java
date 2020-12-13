package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;


import java.io.IOException;
import java.util.List;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {

    ImageButton m_changeToCarActivity;
    ImageButton m_changeToBicyleActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_changeToCarActivity = (ImageButton) findViewById(R.id.imageButton);
        m_changeToBicyleActivity = (ImageButton) findViewById(R.id.imageButton2);

        m_changeToCarActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCarActivity();
            }
        });
        m_changeToBicyleActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBicyleActivity();
            }
        });
    }

    public void openCarActivity(){
        Intent carIntent = new Intent(MainActivity.this, CarActivity.class);
        startActivity(carIntent);
        //finish();
    }
    public void openBicyleActivity(){
        Intent bicyleIntent = new Intent(MainActivity.this, BicycleActivity.class);
        startActivity(bicyleIntent);
        //finish();
    }
}
