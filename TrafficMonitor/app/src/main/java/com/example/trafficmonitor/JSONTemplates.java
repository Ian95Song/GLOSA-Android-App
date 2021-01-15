package com.example.trafficmonitor;

import java.util.List;

// Class Templates for JSON parser, not all but only the key parameters in JSON will be parsed
public class JSONTemplates {
}
// Lanes (from KML converted JSON)
class LanesKML {
    List<LanesFeature> features;
}
class LanesFeature {
    LanesProperties properties;
    Geometry geometry;
}
class LanesProperties {
    String Name;
    int laneId;
    String laneType;
    String directionalUse; // "ingressPath", "egressPath"
}

// Connections (from KML converted JSON)
class ConnectionsKML {
    List<ConnectionsFeature> features;
}
class ConnectionsFeature {
    ConnectionsProperties properties;
    Geometry geometry;
}
class ConnectionsProperties {
    String Name;
    String fromLane;
    String toLane;
    String signalGroup;
    String maneuver; // "maneuverRightAllowed", "maneuverStraightAllowed", "maneuverLeftAllowed"
}
class Geometry {
    List<List<Double>> coordinates;
}

// Map Info (from DCAITI Server requested JSON)
class MapInfo {
    Map map;
}
class Map{
    Intersection intersection;
}
class Intersection{
    PositionWGS84 positionWGS84;
    PositionUTM positionUTM;
    int intersectionID;
    List<Lane> lanes;
}
class Lane{
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

// Spat Info about traffic lights (from DCAITI Server requested JSON)
class Spat {
    Long timestamp;
    List<IntersectionState> intersectionStates;
}
class IntersectionState{
    int intersectionId;
    Long timestamp;
    List<MovementState> movementStates;
}
class MovementState{
    int signalGroupId;
    List<MovementEvent> movementEvents;
}
class MovementEvent{
    String phaseState;
    TimeChange timeChange;
}
class TimeChange{
    int likelyTime;
}
