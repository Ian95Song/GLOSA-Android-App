package com.example.trafficmonitor;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

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
     * Input: json String
     * Return: LanesKML object of lanes
     * Description:
     */
    protected static LanesKML lanesParser(String jsonString) {
        Gson gson = new Gson();
        Type laneType = new TypeToken<LanesKML>(){}.getType();
        LanesKML lanes = gson.fromJson(jsonString, laneType);
        return lanes;
    }

    /*
     * Input: json String
     * Return: ConnectionsKML object of connections
     * Description:
     */
    protected static ConnectionsKML connectionsParser(String jsonString) {
        Gson gson = new Gson();
        Type connectionType = new TypeToken<ConnectionsKML>(){}.getType();
        ConnectionsKML connections = gson.fromJson(jsonString, connectionType);
        return connections;
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

    /*
     * Input: list of location in the past time
     * Return: corresponding signal group
     * Description: do determination job with linear regression
     */
    protected static void doDeterminationLinearRegression(List<UTMLocation> locationList, boolean determinated, List<Lane> trafficLights){
        determinated = true;
        double alpha = Utils.getLinearRegressionAlpha(locationList);
        double beta = Utils.getLinearRegressionBeta(locationList, alpha);
        Log.i("Determination","Linear Regression Alpha Beta: "+alpha+","+beta);
        int laneId = 0;
        int signalGroupId = 0;
        double deltaABS = Double.POSITIVE_INFINITY;
        for (Lane trafficLight : trafficLights){
            if(trafficLight.id == 1 || trafficLight.id == 2 || trafficLight.id == 3 || trafficLight.id == 100){
                double X = trafficLight.positionUTM.east;
                double Y = trafficLight.positionUTM.east;
                double delta = Y - (alpha * X + beta);
                if(Math.abs(delta) < deltaABS){
                    deltaABS = Math.abs(delta);
                    laneId = trafficLight.id;
                    signalGroupId = trafficLight.signalGroup;
                }
                Log.i("Determination","Result lane ID: "+trafficLight.id+", signal group ID:"+trafficLight.signalGroup+"delta: "+delta);

            }
        }
    }

    /*
     * Input: list of location in the past time
     * Return: corresponding signal group
     * Description: do determination job with distance between user location and lane points
     */
    protected static void doDeterminationDistanceToLane(UTMLocation currentLocation, boolean determinated, LanesKML lanes, String mode_selected){
        determinated = true;
        int determinatedLaneId = 0;
        double determinatedDistance = Double.POSITIVE_INFINITY;
        for(LanesFeature feature : lanes.features){
            int laneId = feature.properties.laneId;
            String laneType = feature.properties.laneType;
            double minDistance = Double.POSITIVE_INFINITY;
            if(laneType.equals(mode_selected)){
                for(UTMLocation location : Utils.addPointsToLanes(feature.geometry.coordinates)){
                    double distance = Utils.getUTMDistance(currentLocation, location);
                    if(distance < minDistance){
                        minDistance = distance;
                    }
                }
                if(minDistance < determinatedDistance){
                    determinatedDistance = minDistance;
                    determinatedLaneId = laneId;
                }
            }
        }
        int determinatedSignalGroupId = getSignalGroupOfLane(determinatedLaneId);
        String determinationResult = "Lane: " + determinatedLaneId + " Signal Group: " +determinatedSignalGroupId;
        //Toast.makeText(MainActivity.this, determinationResult, Toast.LENGTH_SHORT).show();
        Log.i("Determination", "Result lane ID: "+determinatedLaneId+" min distance: "+determinatedDistance+" m");
    }

    /*
     * Input: integer of lane id
     * Return: integer of signal group id
     * Description: get corresponding signal group of one lane
     */
    protected static Integer getSignalGroupOfLane(int laneId){
        int signalGroupId = 0;
        //for(Lane lane : trafficLights){
        //    if(lane.id == laneId){
        //        signalGroupId = lane.signalGroup;
        //    }
        //}
        return signalGroupId;
    }

}
