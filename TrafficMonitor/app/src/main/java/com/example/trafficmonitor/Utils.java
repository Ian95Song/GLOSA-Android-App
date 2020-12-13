package com.example.trafficmonitor;
import android.os.Build;

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
import java.util.Base64;

public class Utils {
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getMapInfoJson() throws IOException {
        String mapInfoUrl = AuthUrlInfo.getMapInfoUrl();
        String basicAuth = AuthUrlInfo.getBasicAuth();
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getSpatJson() throws IOException {
        String mapInfoUrl = AuthUrlInfo.getSpatUrl();
        String basicAuth = AuthUrlInfo.getBasicAuth();
        String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes(StandardCharsets.UTF_8));
        URL url = new URL(mapInfoUrl);
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
    public static Spat spatParser(String jsonString){
        Gson gson = new Gson();
        Type spatType = new TypeToken<Spat>(){}.getType();
        Spat spat = gson.fromJson(jsonString, spatType);
        return spat;
    }
    public static MapInfo mapInfoParser(String jsonString){
        Gson gson = new Gson();
        Type mapInfoType = new TypeToken<MapInfo>(){}.getType();
        MapInfo mapInfo = gson.fromJson(jsonString, mapInfoType);
        return mapInfo;
    }
}
