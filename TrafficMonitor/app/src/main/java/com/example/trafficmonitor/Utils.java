package com.example.trafficmonitor;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class Utils {
    private static String _s_username = "username";
    private static String _s_password = "password";

    /*
     * Input: none
     * Return: String of basic authorization information
     * Description:
     */
    private static String getBasicAuth(){
        return _s_username + ":" + _s_password;
    }

    /*
     * Input: String of username and password
     * Return: none
     * Description: set basic authorization information
     */
    protected static void setBasicAuth(String usernameToSet, String passwordToSet){
        _s_username = usernameToSet;
        _s_password = passwordToSet;
    }

    /*
     * Input: String of url address to get json of map information
     * Return: json String of map information
     * Description: asynchronous function
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected static String getMapInfoJson(String mapInfoUrl) throws IOException {
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
    protected static String getSpatJson(String spatUrl) throws IOException {
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
               Spat spat = spatParser(sb.toString());
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
     * Input: json String of spat information
     * Return: Spat object
     * Description:
     */
    protected static Spat spatParser(String jsonString){
        Gson gson = new Gson();
        Type spatType = new TypeToken<Spat>(){}.getType();
        Spat spat = gson.fromJson(jsonString, spatType);
        return spat;
    }

    /*
     * Input: json String of map information
     * Return: MapInfo object
     * Description:
     */
    protected static MapInfo mapInfoParser(String jsonString){
        Gson gson = new Gson();
        Type mapInfoType = new TypeToken<MapInfo>(){}.getType();
        MapInfo mapInfo = gson.fromJson(jsonString, mapInfoType);
        return mapInfo;
    }

    /*
     * Input: UTMLocation objects of two points
     * Return: double number of distance between two input points
     * Description: measurement unit with meter
     */
    protected static double getUTMDistance(UTMLocation location1, UTMLocation location2){
        double distance = Math.sqrt(
                Math.pow(location2.m_east-location1.m_east,2) + Math.pow(location2.m_north-location1.m_north,2));
        return distance;
    }
}
