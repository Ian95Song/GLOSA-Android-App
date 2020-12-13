package com.example.trafficmonitor;

import java.util.List;


public class Spat {
    int msgID;
    int msgSubID;
    Long timestamp;
    List<IntersectionState> intersectionStates;
}
class IntersectionState{
        int intersectionId;
        Long regionId;
        int revision;
        // List<> status;
        Long timestamp;
        int timeshift;
        Long recvtime;
        String source;
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
    int startTime;
    int minEndTime;
    int maxEndTime;
    int likelyTime;
    int confidence;
    int nextTime;
}