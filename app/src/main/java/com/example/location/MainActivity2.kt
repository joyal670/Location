package com.example.location

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.location.databinding.ActivityMain2Binding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.maps.android.PolyUtil
import org.json.JSONObject

class MainActivity2 : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMain2Binding
    private var googleMap: GoogleMap? = null
    var locationManager: LocationManager? = null
    var longitudeBest = 0.0
    var latitudeBest = 0.0

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    private var resolutionForResult: ActivityResultLauncher<IntentSenderRequest>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_map) as SupportMapFragment

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mapFragment.getMapAsync(this)


        if (checkLocation()) {
            toggleBestUpdates()
        }

        resolutionForResult = registerForActivityResult<IntentSenderRequest, ActivityResult>(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                if (checkLocation()) {
                    toggleBestUpdates()
                }
            } else {
                /* permissions not Granted */
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0


    }

    private val locationListenerBest: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            longitudeBest = location.longitude
            latitudeBest = location.latitude

            val latLngOrigin = LatLng(latitudeBest, longitudeBest) // Ayala
            val latLngDestination = LatLng(9.5916,76.5222) // SM City
            googleMap!!.addMarker(MarkerOptions().position(latLngOrigin).title("Ayala"))
            googleMap!!.addMarker(MarkerOptions().position(latLngDestination).title("SM City"))
            googleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngOrigin, 14.5f))
            val path: MutableList<List<LatLng>> = ArrayList()
            val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=10.3181466,123.9029382&destination=10.311795,123.915864&key=AIzaSyAKueYWFVF6M472H_4nPwZEyxhkfNOmj8o"
            val directionsRequest = object : StringRequest(Method.GET, urlDirections, Response.Listener {
                    response ->
                val jsonResponse = JSONObject(response)
                // Get routes
                val routes = jsonResponse.getJSONArray("routes")
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val steps = legs.getJSONObject(0).getJSONArray("steps")
                for (i in 0 until steps.length()) {
                    val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                    path.add(PolyUtil.decode(points))
                }
                for (i in 0 until path.size) {
                    googleMap!!.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
                }
            }, Response.ErrorListener {

            }){}
            val requestQueue = Volley.newRequestQueue(this@MainActivity2)
            requestQueue.add(directionsRequest)
        }

        override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
        override fun onProviderEnabled(s: String) {}
        override fun onProviderDisabled(s: String) {}
    }

    private fun checkLocation(): Boolean {
        if (!isLocationEnabled()) showAlert()
        return isLocationEnabled()
    }

    private fun showAlert() {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, (10 * 1000).toLong())
                .setWaitForAccurateLocation(false).setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
            .addOnSuccessListener(this
            ) { response: LocationSettingsResponse? ->
                toggleBestUpdates()
            }.addOnFailureListener(this
            ) { ex: Exception? ->
                if (ex is ResolvableApiException) {
                    try {
                        val intentSenderRequest =
                            IntentSenderRequest.Builder(ex.resolution)
                                .build()
                        resolutionForResult!!.launch(intentSenderRequest)
                    } catch (exception: Exception) {
                        Toast.makeText(this, "" + exception, Toast.LENGTH_SHORT).show()
                        Log.d("TAG", "enableLocationSettings: $exception")
                    }
                }
            }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager!!.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun toggleBestUpdates() {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.isAltitudeRequired = false
        criteria.isBearingRequired = false
        criteria.isCostAllowed = true
        criteria.powerRequirement = Criteria.POWER_LOW
        val provider = locationManager!!.getBestProvider(criteria, true)
        if (provider != null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Check Permissions Now
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                locationManager!!.requestLocationUpdates(
                    provider,
                    (2 * 60 * 1000).toLong(),
                    10f,
                    locationListenerBest
                )
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkLocation()) {
                    toggleBestUpdates()
                }
            } else {
                // Permission is denied, handle this case or show an explanation to the user
                // ...
            }
        }
    }
}