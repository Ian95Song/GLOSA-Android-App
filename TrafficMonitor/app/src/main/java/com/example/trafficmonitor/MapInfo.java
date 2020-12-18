package com.example.trafficmonitor;

import java.util.List;

public class MapInfo {
    Map map;
}
class Map{
    Intersection intersection;
}
class Intersection{
    PositionWGS84 positionWGS84;
    PositionUTM positionUTM;
    int intersectionID;
    List<Lanes> lanes;
}
class Lanes{
    PositionWGS84 positionWGS84;
    PositionUTM positionUTM;
    int id;
    int signalGroup;
}
class PositionWGS84{
    double lng;
    double lat;
    public PositionWGS84 (double lat, double lng){
        this.lat = lat;
        this.lng = lng;
    }
}
class PositionUTM{
    double east;
    double north;
}