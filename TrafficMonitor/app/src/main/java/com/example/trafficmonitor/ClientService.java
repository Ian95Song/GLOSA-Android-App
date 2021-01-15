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
    public static final String TASK_VERIFY_AUTH = "verifyAuthenticity";
    public static final String TASK_REQ_SPAT_JSON = "requestSpatJson";
    public static final String TASK_READ_LANES_JSON= "readLanesJson";
    public static final String TASK_READ_CONNECTIONS_JSON = "readConnectionsJson";
    public static final String RESP_VERIFY_AUTH = "verifyAuthenticityResp";
    public static final String RESP_REQ_SPAT_JSON = "reqSpatJsonResp";
    public static final String RESP_READ_LANES_JSON = "readLanesJsonResp";
    public static final String RESP_READ_CONNECTIONS_JSON = "readConnectionsJsonResp";
    private ClientBinder _m_Binder;
    private String _m_basicAuth = "username:password";
    private String _m_mapInfoJson;
    private String _m_spatJson;
    private String _m_lanesJson;
    private String _m_connectionsJson;

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
            case TASK_VERIFY_AUTH:
                _m_basicAuth = value;
                new Thread(
                    new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void run() {
                            Intent respIntent = new Intent(LoginActivity.SERVICE_RECEIVER);
                            Bundle resp = new Bundle();
                            resp.putString("task", RESP_VERIFY_AUTH);
                            try {
                                _m_mapInfoJson = reqMapInfoJson(getResources().getString(R.string.map_info_url));
                                resp.putString("value", "true");
                            } catch (Exception e) {
                                e.printStackTrace();
                                resp.putString("value", "false");
                            } finally {
                                respIntent.putExtras(resp);
                                getApplicationContext().sendBroadcast(respIntent);
                            }
                        }
                    }
                ).start();
                break;
            case TASK_REQ_SPAT_JSON:
                new Thread(
                    new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void run() {
                            Intent respIntent = new Intent(MainActivity.SERVICE_RECEIVER);
                            Bundle resp = new Bundle();
                            resp.putString("task", RESP_REQ_SPAT_JSON);
                            try {
                                _m_spatJson = reqSpatJson(getResources().getString(R.string.spat_url));
                                resp.putString("value", "true");
                            } catch (Exception e) {
                                e.printStackTrace();
                                resp.putString("value", "false");
                            } finally {
                                respIntent.putExtras(resp);
                                getApplicationContext().sendBroadcast(respIntent);
                            }
                        }
                    }
                ).start();
                break;
            case TASK_READ_LANES_JSON:
                new Thread(
                    new Runnable() {
                        public void run() {
                            Intent respIntent = new Intent(LoginActivity.SERVICE_RECEIVER);
                            Bundle resp = new Bundle();
                            resp.putString("task", RESP_READ_LANES_JSON);
                            try {
                                _m_lanesJson = readLanesJson();
                                //_m_lanes = Utils.laneParser(sb.toString());
                                resp.putString("value", "true");
                            } catch (IOException e) {
                                e.printStackTrace();
                                resp.putString("value", "false");
                            } finally {
                                respIntent.putExtras(resp);
                                getApplicationContext().sendBroadcast(respIntent);
                            }
                        }
                    }
                ).start();
                break;
            case TASK_READ_CONNECTIONS_JSON:
                new Thread(
                        new Runnable() {
                            public void run() {
                                Intent respIntent = new Intent(LoginActivity.SERVICE_RECEIVER);
                                Bundle resp = new Bundle();
                                resp.putString("task", RESP_READ_CONNECTIONS_JSON);
                                try {
                                    _m_connectionsJson = readConnectionsJson();
                                    //_m_connectionsJson = Utils.connectionsParser(sb.toString());
                                    resp.putString("value", "true");
                                } catch (IOException e) {
                                    e.printStackTrace();
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
            Log.d(TAG, "onBind");
            return _m_Binder;
        }
        return null;
    }
    class ClientBinder extends Binder {
        public String getMapInfoJson() {
            Log.d(TAG, "getMapInfoJson");
            return _m_mapInfoJson;
        }
        public String getSpatJson() {
            Log.d(TAG, "getSpatJson");
            return _m_spatJson;
        }
        public String getLanesJson() {
            Log.d(TAG, "getLanesJson");
            return _m_lanesJson;
        }
        public String getConnectionsJson() {
            Log.d(TAG, "getConnectionsJson");
            return _m_connectionsJson;
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
    private String reqMapInfoJson(String mapInfoUrl) throws IOException {
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
    private String reqSpatJson(String spatUrl) throws IOException {
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

    /*
     * Input: none
     * Return: json String of lanes information
     * Description: get lanes json
     */
    private String readLanesJson() throws IOException {
        InputStream inputStream = getAssets().open("14052_lanes.json");
        InputStreamReader isReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(isReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
            sb.append(str);
        }
        return sb.toString();
    }

    /*
     * Input: none
     * Return: json String of connections information
     * Description: get connections json
     */
    private String readConnectionsJson() throws IOException {
        InputStream inputStream = getAssets().open("14052_connections.json");
        InputStreamReader isReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(isReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
            sb.append(str);
        }
        return sb.toString();
    }
}