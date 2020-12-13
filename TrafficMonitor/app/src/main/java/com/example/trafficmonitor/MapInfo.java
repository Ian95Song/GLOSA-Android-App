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
    positionUTM positionUTM;
    int intersectionID;
    List<Lanes> lanes;
}
class Lanes{
    PositionWGS84 positionWGS84;
    positionUTM positionUTM;
    int id;
    int signalGroup;
}
class PositionWGS84{
    double lng;
    double lat;
}
class positionUTM{
    double east;
    double north;
}