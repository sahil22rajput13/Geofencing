package com.example.geofencing

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.geofencing.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : FragmentActivity(), OnMapReadyCallback{
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private  val radius = 200F
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)
        checkFineLocationPermission()
    }

    private fun supportMapFrag() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

    }

    @SuppressLint("InlinedApi")
    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Notification permission granted, you can proceed with notification tasks
                supportMapFrag()
            } else {
                // Notification permission denied, show a dialog or navigate to app settings
                showPermissionDeniedDialog()
            }
        }

    private fun checkFineLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
            supportMapFrag()
            checkCoarseLocationPermission()
        }
    }
    private fun checkCoarseLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            coarseLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }else{
            supportMapFrag()
            checkBackgroundLocationPermission()
        }
    }
    @SuppressLint("InlinedApi")
    private fun checkBackgroundLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }else{
            supportMapFrag()
        }
    }
    private val fineLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                supportMapFrag()
                // Permission granted, you can proceed with fine location tasks

            } else {
                // Permission denied, show a dialog or navigate to app settings
                showPermissionDeniedDialog()
            }
        }

    private val coarseLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, you can proceed with coarse location tasks
                supportMapFrag()
            } else {
                // Permission denied, show a dialog or navigate to app setting
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val backgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, you can proceed with background location tasks
               supportMapFrag()
            } else {
                // Permission denied, show a dialog or navigate to app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                Toast.makeText(this, "Require location permission all the time ", Toast.LENGTH_SHORT).show()
            }
        }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Please grant the necessary permissions in the app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableUserLocation()
        addGeofence(LatLng(30.6987360, 76.6911450))
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkNotificationPermission()
            mMap.isMyLocationEnabled = true
            // Create a location request
            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(100)
                .build()

            // Create a location callback
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    val latLng = LatLng(location.latitude, location.longitude)

                    mMap.apply {
                        animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

        } else {
           checkFineLocationPermission()
        }
    }



    @SuppressLint("MissingPermission")
    private fun addGeofence(latLng: LatLng) {
        val geofence = Geofence.Builder()
            .setRequestId("unique_id") // A unique ID for this geofence
            .setCircularRegion(30.6987360, 76.6911450, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        val geofencePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_MUTABLE
        )


        if (geofencePendingIntent != null) {
                try {
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).addOnSuccessListener {

                        Log.d(
                            "MapsActivity",
                            "onSuccess: Added..."
                        )
                        markGeofencing(latLng)
                        circleGeofencing(latLng)
                    }.addOnFailureListener { e ->
                        val errorMessage: String = geofenceHelper.getErrorString(e).toString()
                        Log.d("MapsActivity", "onFailure: $errorMessage")
                    }
                }catch (e: Exception) {
                    Log.d("MapsActivity", "onFailure : $e")
                }

        }

    }

    private fun circleGeofencing(latLng: LatLng) {
        val circleOptions = CircleOptions()
            .center(latLng)
            .radius(radius.toDouble())
            .strokeColor(Color.argb(64, 255, 0, 0))
            .fillColor(Color.argb(64, 255, 0, 0))
            .strokeWidth(4f)
        mMap.addCircle(circleOptions)
    }

    private fun markGeofencing(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng).title("Geofence Marker")
        mMap.addMarker(markerOptions)
    }

}