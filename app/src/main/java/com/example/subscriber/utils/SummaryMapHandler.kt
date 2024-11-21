package com.example.subscriber.utils

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import com.example.subscriber.data.DatabaseHelper
import com.example.subscriber.data.StudentData
import com.example.subscriber.ui.SummaryActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class SummaryMapHandler(
    private val context: Context,
    private val googleMap: GoogleMap,
    private val databaseHelper: DatabaseHelper,
    private val studentId: String,
    private var startDate: Long,
    private var endDate: Long
) {

    fun drawMarkersAndPolyline() {
        clearMap()
        val locationDataList = databaseHelper.getLocationDataByStudentIdAndDateRange(studentId, startDate, endDate)
        if (locationDataList.isNotEmpty()) {
            val latLngPoints = locationDataList.map { LatLng(it.latitude, it.longitude) }
            val polylineOptions = PolylineOptions()
                .addAll(latLngPoints)
                .color(Color.BLUE)
                .width(5f)
                .geodesic(true)
            googleMap.addPolyline(polylineOptions)

            val boundsBuilder = LatLngBounds.builder()
            latLngPoints.forEach {
                boundsBuilder.include(it)
                googleMap.addMarker(MarkerOptions().position(it).title("Marker at ${it.latitude}, ${it.longitude}"))
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 10))

            // Update speed values
            updateSpeedValues(locationDataList)
        } else {
            Log.d("SummaryMapHandler", "No LatLng points to display on the map.")
            Toast.makeText(context, "No location data available for the selected date range.", Toast.LENGTH_SHORT).show()
            // Set speeds to 0 if no data points are found
            updateSpeedValues(emptyList())
        }
    }

    fun updateMarkersAndPolyline(newStartDate: Long, newEndDate: Long) {
        startDate = newStartDate
        endDate = newEndDate
        drawMarkersAndPolyline()
    }

    private fun clearMap() {
        googleMap.clear()
    }

    private fun updateSpeedValues(locationDataList: List<StudentData>) {
        val speeds = locationDataList.map { it.speed }
        val minSpeed = speeds.minOrNull() ?: 0.0
        val maxSpeed = speeds.maxOrNull() ?: 0.0
        val avgSpeed = if (speeds.isNotEmpty()) speeds.average() else 0.0

        val activity = context as SummaryActivity
        activity.updateSpeedTextViews(minSpeed, maxSpeed, avgSpeed)
    }
}