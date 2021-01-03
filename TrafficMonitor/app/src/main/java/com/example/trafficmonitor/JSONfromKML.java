package com.example.trafficmonitor;

import java.util.List;

public class JSONfromKML {
    String type;
    String name;
    //crs;
    List<Feature> features;
}
class Feature {
    String type;
    Properties properties;
    Geometry geometry;

}
class Properties {
    String Name;
    int laneId;
    String laneType;
    // more parameters;

}
class Geometry {
    String type;
    List<List<Double>> coordinates;
}