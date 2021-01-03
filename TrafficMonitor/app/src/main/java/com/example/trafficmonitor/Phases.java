package com.example.trafficmonitor;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class CurrentState {
    String state;
    int timeLefts; // unit is second
    int signalGroupId;
    PositionWGS84 postionWGS;
    public CurrentState(String state, int timeLefts, int signalGroupId){
        this.state = state;
        this.timeLefts = timeLefts;
        this.signalGroupId = signalGroupId;
    }

    public void setPostionWGS(List<Lane> lanes) {
        for(int i = 0; i < lanes.size(); i++){
            if(lanes.get(i).signalGroup == this.signalGroupId){
                this.postionWGS = lanes.get(i).positionWGS84;
                break;
            }
        }
    }
}

public class Phases {
    private int signalGroupId;
    private int firstStateLikelyTime; // second in current hour
    private List<String> stateSequence;
    private List<Integer> relativeLikelyTime; // relativ second in current circulation
    private int circulationTime;
    private CurrentState currentState;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Phases(Long timestamp, int signalGroupId, List<MovementEvent> movementEvents){
        this.signalGroupId = signalGroupId;
        this.stateSequence = new ArrayList<>();
        this.relativeLikelyTime = new ArrayList<>();
        this.firstStateLikelyTime = movementEvents.get(0).timeChange.likelyTime / 10;
        this.stateSequence.add(movementEvents.get(0).phaseState);
        this.relativeLikelyTime.add(0);
        for(int i = 1; i < 4; i++) {
            this.stateSequence.add(movementEvents.get(i).phaseState);
            this.relativeLikelyTime.add(movementEvents.get(i).timeChange.likelyTime/10 -this.firstStateLikelyTime);
        }
        this.circulationTime = 0;
        this.circulationTime = movementEvents.get(4).timeChange.likelyTime/10 -this.firstStateLikelyTime;
    }
    public void calculateCurrentState(){
        Timestamp timestamp = new Timestamp(new Date().getTime());
        //second in the current hour
        int time = timestamp.getMinutes() * 60 + timestamp.getSeconds();
        String state;
        int timeLefts;
        int modulo = (int) ((time - this.firstStateLikelyTime) % this.circulationTime);
        if(modulo >= 0){
            if(modulo < this.relativeLikelyTime.get(1)){
                state = this.stateSequence.get(1);
                timeLefts = this.relativeLikelyTime.get(1) - modulo;
            } else if(modulo >= this.relativeLikelyTime.get(1) && modulo < this.relativeLikelyTime.get(2)){
                state = this.stateSequence.get(2);
                timeLefts = this.relativeLikelyTime.get(2) - modulo;
            } else if(modulo >= this.relativeLikelyTime.get(2) && modulo < this.relativeLikelyTime.get(3)){
                state = this.stateSequence.get(3);
                timeLefts = this.relativeLikelyTime.get(3) - modulo;
            } else{
                state = this.stateSequence.get(0);
                timeLefts = this.circulationTime - modulo;
            }
        } else {
            if(modulo + this.circulationTime < this.relativeLikelyTime.get(1)){
                state = this.stateSequence.get(1);
                timeLefts = this.relativeLikelyTime.get(1) - (this.circulationTime + modulo);
            } else if(modulo + this.circulationTime >= this.relativeLikelyTime.get(1) && modulo + this.circulationTime < this.relativeLikelyTime.get(2)){
                state = this.stateSequence.get(2);
                timeLefts = this.relativeLikelyTime.get(2) - (this.circulationTime + modulo);
            } else if(modulo + this.circulationTime >= this.relativeLikelyTime.get(2) && modulo + this.circulationTime < this.relativeLikelyTime.get(3)){
                state = this.stateSequence.get(3);
                timeLefts = this.relativeLikelyTime.get(3) - (this.circulationTime + modulo);
            } else{
                state = this.stateSequence.get(0);
                timeLefts = -modulo;
            }

        }
        this.currentState = new CurrentState(state,timeLefts,this.signalGroupId);
    }
    public CurrentState getCurrentState(){
        this.calculateCurrentState();
        return this.currentState;
    }
}
