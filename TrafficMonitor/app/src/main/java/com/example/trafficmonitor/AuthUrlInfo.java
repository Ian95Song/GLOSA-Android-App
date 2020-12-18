package com.example.trafficmonitor;

public class AuthUrlInfo {
    private static String username ="username";
    private static String password="password";
    protected static void setBasicAuth(String username_set, String password_set){
        username = username_set;
        password = password_set;
    }
    public static String getBasicAuth(){
        return username + ":" + password;
    }
    public static String getSpatUrl(){
        return "https://werkzeug.dcaiti.tu-berlin.de/0432l770/trafficlights/spats?intersection=14052@berlin";
    }
    public static String getMapInfoUrl(){
        return "https://werkzeug.dcaiti.tu-berlin.de/0432l770/maps/download?intersection=14052@berlin";
    }

}
