package com.example.dcaiti_ws2020

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.data.kml.KmlLayer
import java.io.InputStream


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("ResourceType")
    override fun onMapReady(googleMap: GoogleMap) {
        val jsonFileString = getJsonDataFromAsset(applicationContext, "example_raw_data.json")
        val gson = Gson()
        val spatType = object : TypeToken<spat>() {}.type
        var spat: spat = gson.fromJson(jsonFileString, spatType)
        var signalGroups = spat.intersectionStates[0].movementStates

        mMap = googleMap
        val intersection = LatLng(52.564232999999994, 13.327774999999999)
        mMap.addMarker(MarkerOptions().position(intersection).title("Reference Point of Intersection 14052"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(intersection, 19f))
        val markerManager = MarkerManager(mMap)
        val layer = KmlLayer(mMap, R.raw.signalgroups, applicationContext, markerManager, null, null, null, null)
        layer.addLayerToMap()
        val markerCollection = markerManager.newCollection()
        var signalGroupsSetted = ArrayList<String>()
        for (containerNest in layer.containers) {
            if (containerNest.hasContainers()) {
                for (container in containerNest.containers) {
                    for (placemark in container.placemarks) {
                        val latlng = placemark.geometry.geometryObject as LatLng
                        val name = placemark.getProperty("name")
                        val signalGroupId = name.split(":")[0]
                        if(signalGroupsSetted.indexOf(signalGroupId) == -1){
                            markerCollection.addMarker(
                                MarkerOptions()
                                    .position(latlng)
                                    .icon(BitmapDescriptorFactory.fromBitmap(signalGroupMatch(signalGroupId, signalGroups)))
                                    .title(name)
                            )
                            signalGroupsSetted.add(signalGroupId)
                        }
                    }
                }
            }
        }
        layer.removeLayerFromMap()
    }
    @SuppressLint("ResourceType")
    fun signalGroupMatch(signalGroupId: String, signalGroupStatus: List<movementState>): Bitmap{
        var signalGroupMovementEvent = "UNAVAILABLE"
        var sourceId = R.mipmap.dark
        for(movementState in signalGroupStatus){
            if(movementState.signalGroupId == signalGroupId.toInt()){
                signalGroupMovementEvent = movementState.movementEvents[0].phaseState
            }
        }
        when(signalGroupMovementEvent){
            "STOP_AND_REMAIN" ->   sourceId = R.mipmap.stop_and_remain
            "PRE_MOVEMENT" -> sourceId = R.mipmap.pre_movement
            "PROTECTED_MOVEMENT_ALLOWED" -> sourceId = R.mipmap.protected_movement_allowed
            "PROTECTED_CLEARANCE" -> sourceId = R.mipmap.protected_clearance
            "DARK" -> sourceId = R.mipmap.dark
            else -> Log.e("spat json", "undefined movement event$signalGroupMovementEvent")
        }
        val inputs: InputStream = this.getResources().openRawResource(sourceId)
        val bitmap = BitmapFactory.decodeStream(inputs)
        return Bitmap.createScaledBitmap(bitmap, bitmap.width/2, bitmap.height/2,true)
    }
}