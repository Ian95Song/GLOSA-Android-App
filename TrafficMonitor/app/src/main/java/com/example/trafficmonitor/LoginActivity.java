package com.example.trafficmonitor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    public static final String TAG = "LoginActivity";
    public static final String SERVICE_RECEIVER = "com.example.trafficmonitor.RECEIVE_SERVICE";
    private EditText _m_eText_username;
    private EditText _m_eText_password;
    private String _m_username = null;
    private String _m_password = null;
    private Intent _m_serviceIntent;
    private ClientReceiver _m_clientReceiver;

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

        _m_serviceIntent = new Intent(this, ClientService.class);
        _m_clientReceiver = new ClientReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SERVICE_RECEIVER);
        registerReceiver(_m_clientReceiver, intentFilter);

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
                _m_username=_m_eText_username.getText().toString();
                _m_password=_m_eText_password.getText().toString();
                if (rememberCheckbox.isChecked()){
                    writeAuthToLocal(_m_username, _m_password);
                }

                Bundle extras = new Bundle();
                extras.putString("task", ClientService.TASK_VERIFY_AUTH);
                extras.putString("value",  _m_username+":"+_m_password);
                _m_serviceIntent.putExtras(extras);
                startService(_m_serviceIntent);

            }
        });
        Bundle extrasLanes = new Bundle();
        extrasLanes.putString("task", ClientService.TASK_READ_LANES_JSON);
        _m_serviceIntent.putExtras(extrasLanes);
        startService(_m_serviceIntent);

        Bundle extrasConnections = new Bundle();
        extrasConnections.putString("task", ClientService.TASK_READ_CONNECTIONS_JSON);
        _m_serviceIntent.putExtras(extrasConnections);
        startService(_m_serviceIntent);

        readAuthFromLocal();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(_m_clientReceiver);
        super.onDestroy();
    }
    public class ClientReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (SERVICE_RECEIVER.equals(action)) {
                    Bundle resp = intent.getExtras();
                    String task = (String) resp.get("task");
                    String value = (String) resp.get("value");
                    Log.d(TAG, task+":"+value);
                    switch (task) {
                        case ClientService.RESP_READ_LANES_JSON:
                            if(Boolean.valueOf(value)){
                                Toast.makeText(LoginActivity.this, "Loading Lanes JSON Successfully", Toast.LENGTH_SHORT).show();

                            } else {
                                Toast.makeText(LoginActivity.this, "Loading Lanes JSON Failed", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case ClientService.RESP_READ_CONNECTIONS_JSON:
                            if(Boolean.valueOf(value)){
                                Toast.makeText(LoginActivity.this, "Loading Connections JSON Successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Loading Connections JSON Failed", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case ClientService.RESP_VERIFY_AUTH:
                            if(Boolean.valueOf(value)){
                                Toast.makeText(LoginActivity.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                                Intent homeIntent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(homeIntent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Invalid Authorization or Server Down", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }
            }
        }
    }

    /*
     * Input: none
     * Return: none
     * Description: try to read authorization information from local,
     *              and if get it, update Edit Text and variables in background
     */
    private void readAuthFromLocal(){
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
        }
    }

    /*
     * Input: String of username and password
     * Return: none
     * Description: write authorization information to local
     */
    private void writeAuthToLocal(String username, String password){
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