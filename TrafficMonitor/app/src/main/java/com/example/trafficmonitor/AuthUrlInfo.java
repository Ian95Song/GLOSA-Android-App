package com.example.trafficmonitor;

public class AuthUrlInfo {
    public static String getBasicAuth(){
        return "username" + ":" + "password";
    }
    public static String getSpatUrl(){
        return "https://werkzeug.dcaiti.tu-berlin.de/0432l770/trafficlights/spats?intersection=14052@berlin";
    }public static String getMapInfoUrl(){
        return "https://werkzeug.dcaiti.tu-berlin.de/0432l770/maps/download?intersection=14052@berlin";
    }

}
