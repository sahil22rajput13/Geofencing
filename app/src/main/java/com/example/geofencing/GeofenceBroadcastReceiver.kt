package com.example.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Display a toast message indicating that a geofence event was triggered
        Toast.makeText(context, "Geofence triggered", Toast.LENGTH_SHORT).show()

        // Create a NotificationHelper instance for displaying notifications
        val notificationHelper = NotificationHelper(context)

        // Parse the GeofencingEvent from the received intent
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        // Check for errors in the geofencing event
        try {
            if (geofencingEvent == null || geofencingEvent.hasError()) {
                Log.d("GeofenceBroadcastReceiver", "onReceive: Error receiving geofence event...")
                return
            }

        }catch (e:Exception)
        {
            Log.e("GeofenceBroadcastReceiver", e.message.toString())
        }

        // Get the list of triggering geofences
        val geofenceList = geofencingEvent?.triggeringGeofences

        // Check if any triggering geofences were found
        if (geofenceList.isNullOrEmpty()) {
            Log.d("GeofenceBroadcastReceiver", "onReceive: No triggering geofences found.")
            notificationHelper.createNotification("No triggering geofences found", "Geofence")
            return
        }

        // Loop through each triggering geofence and log its request ID
        for (geofence in geofenceList) {
            Log.d("GeofenceBroadcastReceiver", "onReceive: Triggered Geofence - Request ID: ${geofence.requestId}")
        }

        // Handle geofence transition types
        when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                // Handle geofence enter event
                Toast.makeText(context, "Entered Geofence", Toast.LENGTH_SHORT).show()
                notificationHelper.createNotification("Entered Geofence", "Notification Message")
                Log.d("GeofenceBroadcastReceiver", "onReceive: Entered triggering geofences found.")
            }

            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                // Handle geofence dwell event
                Toast.makeText(context, "Dwelling in Geofence", Toast.LENGTH_SHORT).show()
                notificationHelper.createNotification("Dwelling in Geofence", "Notification Message")
                Log.d("GeofenceBroadcastReceiver", "onReceive: Dwelling triggering geofences found.")
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                // Handle geofence exit event
                Toast.makeText(context, "Exited Geofence", Toast.LENGTH_SHORT).show()
                notificationHelper.createNotification("Exited Geofence", "Notification Message")
                Log.d("GeofenceBroadcastReceiver", "onReceive: Exited triggering geofences found.")
            }
        }
    }

}
