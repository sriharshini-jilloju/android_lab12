package com.city.tourist

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.city.tourist.ui.theme.CityTouristGuideTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : ComponentActivity(), OnMapReadyCallback, View.OnClickListener {

    private lateinit var mapView : MapView
    private lateinit var googleMap : GoogleMap
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private var MAP_VIEW_BUNDLE_KEY = "mapViewBundleKey"
    private var userLatLng : LatLng? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        var recenterButton : Button = findViewById(R.id.recenterButton);
        var mapViewBundle : Bundle ? = savedInstanceState?.getBundle(MAP_VIEW_BUNDLE_KEY);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        recenterButton.setOnClickListener(this)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
        addStaticMarkers()
    }

    override fun onClick(v: View?) {
        userLatLng?.let {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun enableMyLocation(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
           return
        }
        googleMap.isMyLocationEnabled = true;
        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
            location?.let {
                run {
                    userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng!!, 14f))
                    fetchNearbyPlaces(userLatLng!!)
                }
            }
        }
    }

    private fun fetchNearbyPlaces(latLng : LatLng){
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${latLng.latitude},${latLng.longitude}" +
                "&radius=1500&type=restaurant&key=AIzaSyC4N6-D8C0hpMuCYWuNUN4MoiW8Xf48cYM"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val results = response.getJSONArray("results")
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val name = obj.getString("name")
                    val location = obj.getJSONObject("geometry").getJSONObject("location")
                    val lat = location.getDouble("lat")
                    val lng = location.getDouble("lng")
                    googleMap.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(name))
                }
            },
            { error ->
                Toast.makeText(this, "Volley error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(this).add(request)
    }

    private fun addStaticMarkers(){
        val staticPlaces = listOf(
            TouristPlace("Museum of Art", 43.6677, -79.3948, "Museum", "Modern art museum"),
            TouristPlace("Central Park", 43.6735, -79.3871, "Park", "Beautiful city Park")
        )

        for(place in staticPlaces){
            val position = LatLng(place.latitude, place.longitude)
            googleMap.addMarker(MarkerOptions().position(position).title(place.name))
        }
    }
}
