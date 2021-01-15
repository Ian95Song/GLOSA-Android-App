package com.example.trafficmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

public class LocationReceiver extends BroadcastReceiver {
    public static final String ACTION_PROCESS_UPDATE = "com.example.trafficmonitor.UPDATE_LOCATION";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATE.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    Location location = result.getLastLocation();
                    if(MainActivity.getInstance() != null) {
                        MainActivity.getInstance().updateLocationWGS(location);
                    }
                }
            }
        }
    }
}
class UTMLocation {
    double m_east;
    double m_north;
    int m_zone;
    char m_letter;
    public UTMLocation(){
        this.m_east = 0.0;
        this.m_north = 0.0;
    }
    public UTMLocation(double east, double north){
        this.m_east = east;
        this.m_north = north;
    }
    public void getUTMLocationFromWGS (double Lat,double Lon) {
        m_zone= (int) Math.floor(Lon/6+31);
        if (Lat<-72)
            m_letter='C';
        else if (Lat<-64)
            m_letter='D';
        else if (Lat<-56)
            m_letter='E';
        else if (Lat<-48)
            m_letter='F';
        else if (Lat<-40)
            m_letter='G';
        else if (Lat<-32)
            m_letter='H';
        else if (Lat<-24)
            m_letter='J';
        else if (Lat<-16)
            m_letter='K';
        else if (Lat<-8)
            m_letter='L';
        else if (Lat<0)
            m_letter='M';
        else if (Lat<8)
            m_letter='N';
        else if (Lat<16)
            m_letter='P';
        else if (Lat<24)
            m_letter='Q';
        else if (Lat<32)
            m_letter='R';
        else if (Lat<40)
            m_letter='S';
        else if (Lat<48)
            m_letter='T';
        else if (Lat<56)
            m_letter='U';
        else if (Lat<64)
            m_letter='V';
        else if (Lat<72)
            m_letter='W';
        else
            m_letter='X';
        m_east=0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*m_zone-183)*Math.PI/180))/(1-Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*m_zone-183)*Math.PI/180)))*0.9996*6399593.62/Math.pow((1+Math.pow(0.0820944379, 2)*Math.pow(Math.cos(Lat*Math.PI/180), 2)), 0.5)*(1+ Math.pow(0.0820944379,2)/2*Math.pow((0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*m_zone-183)*Math.PI/180))/(1-Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*m_zone-183)*Math.PI/180)))),2)*Math.pow(Math.cos(Lat*Math.PI/180),2)/3)+500000;
        m_east=Math.round(m_east*100)*0.01;
        m_north = (Math.atan(Math.tan(Lat*Math.PI/180)/Math.cos((Lon*Math.PI/180-(6*m_zone -183)*Math.PI/180)))-Lat*Math.PI/180)*0.9996*6399593.625/Math.sqrt(1+0.006739496742*Math.pow(Math.cos(Lat*Math.PI/180),2))*(1+0.006739496742/2*Math.pow(0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin((Lon*Math.PI/180-(6*m_zone -183)*Math.PI/180)))/(1-Math.cos(Lat*Math.PI/180)*Math.sin((Lon*Math.PI/180-(6*m_zone -183)*Math.PI/180)))),2)*Math.pow(Math.cos(Lat*Math.PI/180),2))+0.9996*6399593.625*(Lat*Math.PI/180-0.005054622556*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+4.258201531e-05*(3*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2))/4-1.674057895e-07*(5*(3*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2))/4+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2)*Math.pow(Math.cos(Lat*Math.PI/180),2))/3);
        if (m_letter<'M')
            m_north = m_north + 10000000;
        m_north=Math.round(m_north*100)*0.01;
    }

}
