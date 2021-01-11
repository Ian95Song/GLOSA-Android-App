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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Utils {

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
     * Input: String path of json file
     * Return: JSONfromKML object of lane
     * Description:
     */
    protected static JSONfromKML laneParser(String jsonString) {
        Gson gson = new Gson();
        Type laneType = new TypeToken<JSONfromKML>(){}.getType();
        JSONfromKML lane = gson.fromJson(jsonString, laneType);
        return lane;
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

    /*
     * Input: UTMLocation objects of points
     * Return: double number of linear regression alpha
     * Description: calculate parameter of linear regression: y = ax + b, x: east, y: north
     */
    protected static double getLinearRegressionAlpha(List<UTMLocation> locationList){
        int n = locationList.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;
        for (UTMLocation location : locationList){
            sumX += location.m_east;
            sumY += location.m_north;
            sumXY += location.m_east * location.m_north;
            sumXX += Math.pow(location.m_east, 2);
        }
        double alpha = (((sumY * sumX) / n) - sumXY) / (((sumX * sumX) / n) - sumXX);
        return alpha;
    }

    /*
     * Input: UTMLocation objects of points
     * Return: double number of linear regression beta
     * Description: calculate parameter of linear regression: y = ax + b, x: east, y: north
     */
    protected static double getLinearRegressionBeta(List<UTMLocation> locationList, double alpha){
        int n = locationList.size();
        double sumX = 0;
        double sumY = 0;
        for (UTMLocation location : locationList){
            sumX += location.m_east;
            sumY += location.m_north;
        }
        double beta = (sumY - alpha * sumX) / n;
        return beta;
    }

    /*
     * Input: none
     * Return: none
     * Description: add points into lanes, e.g. add the middle point of two neighbouring points on a lane
     */
    protected static List<UTMLocation> addPointsToLanes(List<List<Double>> coordinates){
        List<UTMLocation> rawLocations = new ArrayList<>();
        for(List<Double> coordinate : coordinates){
            UTMLocation utmLocation = new UTMLocation();
            utmLocation.getUTMLocationFromWGS(coordinate.get(1),coordinate.get(0));
            rawLocations.add(utmLocation);
        }

        List<UTMLocation> addedPointsLocations = new ArrayList<>();
        for(int i = 0; i < rawLocations.size(); i++){
            addedPointsLocations.add(rawLocations.get(i));
            if(i < rawLocations.size() - 1){
                double middleEast = (rawLocations.get(i).m_east + rawLocations.get(i+1).m_east) / 2;
                double middleNorth = (rawLocations.get(i).m_north + rawLocations.get(i+1).m_north) / 2;
                UTMLocation middleLocation = new UTMLocation(middleEast, middleNorth);
                addedPointsLocations.add(middleLocation);
            }
        }
        return addedPointsLocations;
    }
}
