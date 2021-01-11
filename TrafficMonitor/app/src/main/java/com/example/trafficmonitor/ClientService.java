package com.example.trafficmonitor;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class ClientService extends Service {
    public static final String TAG = "ClientService";
    private ClientBinder _m_Binder;
    private String _m_basicAuth = "username:password";
    private String _m_mapInfoJson;
    private String _m_spatJson;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        _m_Binder = new ClientBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        Bundle req = intent.getExtras();
        String task = (String)req.get("task");
        String value = (String) req.get("value");
        Log.d(TAG,task+":"+value);
        switch (task) {
            case "verifyAuthenticity":
                _m_basicAuth = value;
                new Thread(
                    new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void run() {
                            Intent respIntent = new Intent(LoginActivity.SERVICE_RECEIVER);
                            Bundle resp = new Bundle();
                            resp.putString("task", "verifyAuthenticityResp");
                            try {
                                _m_mapInfoJson = getMapInfoJson(getResources().getString(R.string.map_info_url));
                                resp.putString("value", "true");
                            } catch (Exception e) {
                                resp.putString("value", "false");
                            } finally {
                                respIntent.putExtras(resp);
                                getApplicationContext().sendBroadcast(respIntent);
                            }
                        }
                    }
                ).start();
                break;
            case "getSpatJson":
                new Thread(
                    new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void run() {
                            Intent respIntent = new Intent(MainActivity.SERVICE_RECEIVER);
                            Bundle resp = new Bundle();
                            resp.putString("task", "getSpatJsonResp");
                            try {
                                _m_spatJson = getSpatJson(getResources().getString(R.string.spat_url));
                                resp.putString("value", "true");
                            } catch (Exception e) {
                                resp.putString("value", "false");
                            } finally {
                                respIntent.putExtras(resp);
                                getApplicationContext().sendBroadcast(respIntent);
                            }
                        }
                    }
                ).start();
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (_m_Binder != null) {
            return _m_Binder;
        }
        return null;
    }
    class ClientBinder extends Binder {
        public void doTask() {
            Log.d(TAG, "doTask");
        }
    }

    /*
     * Input: none
     * Return: String of basic authorization information
     * Description:
     */
    private String getBasicAuth(){
        return _m_basicAuth;
    }

    /*
     * Input: String of url address to get json of map information
     * Return: json String of map information
     * Description: asynchronous function
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getMapInfoJson(String mapInfoUrl) throws IOException {
        String basicAuth = getBasicAuth();
        String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes(StandardCharsets.UTF_8));
        URL url = new URL(mapInfoUrl);
        URLConnection con = url.openConnection();
        con.setRequestProperty("Authorization", encodedAuth);
        InputStream is = con.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder(); int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        rd.close();
        is.close();
        return sb.toString();
    }

    /*
     * Input: String of url address to get json of spat information
     * Return: json String of spat information
     * Description: asynchronous function
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getSpatJson(String spatUrl) throws IOException {
        String basicAuth = getBasicAuth();
        String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes(StandardCharsets.UTF_8));
        URL url = new URL(spatUrl);
        URLConnection con = url.openConnection();
        con.setRequestProperty("Authorization", encodedAuth);
        InputStream is = con.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
            try {
                Spat spat = Utils.spatParser(sb.toString());
                if(spat.intersectionStates.get(0).movementStates.size() == 1){
                    sb = new StringBuilder();
                    continue;
                } else if (spat.intersectionStates.get(0).movementStates.size() == 11){
                    break;
                }
            } catch (Exception e) {
                continue;
            }
        }
        rd.close();
        is.close();
        return sb.toString();
    }
}