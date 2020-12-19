package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
    private EditText _m_eText_username;
    private EditText _m_eText_password;
    String m_username = null;
    String m_password = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Login");

        Button button = findViewById(R.id.loginButton);
        _m_eText_username = findViewById(R.id.loginEditTextUserName);
        _m_eText_password = findViewById(R.id.loginEditTextPassWord);
        _m_eText_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
        CheckBox rememberCheckbox = findViewById(R.id.loginCheckBoxRemember);
        CheckBox showCheckbox = findViewById(R.id.loginCheckBoxShow);
        readAuthFromLocal();

        showCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    _m_eText_password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }else{
                    _m_eText_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                m_username=_m_eText_username.getText().toString();
                m_password=_m_eText_password.getText().toString();
                if (rememberCheckbox.isChecked()){
                    writeAuthToLocal(m_username, m_password);
                }
                // update Auth
                Utils.setBasicAuth(m_username, m_password);
                // check Auth
                Runnable runnable = new Runnable() {
                    public void run() {
                        try {
                            String mapInfoStr = Utils.getMapInfoJson(getResources().getString(R.string.map_info_url));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                                    Intent homeIntent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(homeIntent);
                                    finish();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Invalid Authorization or Server down", Toast.LENGTH_SHORT).show();

                                }
                            });
                        }
                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
            }
        });
    }

    /*
     * Input: none
     * Return: none
     * Description: try to read authorization information from local,
     *              and if get it, update Edit Text and variables in background
     */
    public void readAuthFromLocal(){
        // read file
        File file = new File(getFilesDir(),"Authorization.txt");
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
            _m_eText_username.setText(up[0]);
            _m_eText_password.setText(up[1]);
            // update Auth
            Utils.setBasicAuth(up[0], up[1]);
        }
    }

    /*
     * Input: String of username and password
     * Return: none
     * Description: write authorization information to local
     */
    public void writeAuthToLocal(String username, String password){
        // write file
        File file = new File(getFilesDir(),"Authorization.txt");
        //Log.i("Auth",file.getAbsolutePath());
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
}