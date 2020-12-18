package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;


public class LoginActivity extends AppCompatActivity {
    private EditText mUsername;
    private EditText mPassWord;
    String username=null;
    String password=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button mButton=(Button)findViewById(R.id.mButton);
        mUsername=(EditText)findViewById(R.id.mUserName);
        mPassWord=(EditText)findViewById(R.id.mPassWord);
        final CheckBox mCheckbox=(CheckBox)findViewById(R.id.mCheckBox);
        readfrominfo();
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCheckbox.isChecked()){
                    username=mUsername.getText().toString();
                    password=mPassWord.getText().toString();
                    File file = new File(getFilesDir(),"info.txt");
                    Log.i("Auth",file.getAbsolutePath());
                    FileOutputStream fos= null;
                    try {
                        fos = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    try {
                        fos.write((username+"##"+password).getBytes());
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Toast.makeText(LoginActivity.this,"login successfully", Toast.LENGTH_SHORT).show();
                Intent homeIntent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(homeIntent);
                finish();
            }
        });
    }
    public void readfrominfo(){
        File file = new File(getFilesDir(),"info.txt");
        if(file.exists()){
            BufferedReader br= null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String str = null;
            try {
                str = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String [] up= str.split("##");

            mUsername.setText(up[0]);
            mPassWord.setText(up[1]);
        }
    }
}