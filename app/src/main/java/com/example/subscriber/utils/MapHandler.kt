package com.example.subscriber.utils

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import android.graphics.Color
import com.example.subscriber.data.DatabaseHelper
import com.example.subscriber.data.StudentData

class MapHandler(
    private val googleMap: GoogleMap,
    private val databaseHelper: DatabaseHelper
) {

    private val studentColors = mutableMapOf<String, Int>()
    private var hasInitialCameraMoved = false

    fun initializeMap() {
        val studentIds = databaseHelper.getAllUniqueStudentIds()
        studentIds.forEach { studentId ->
            drawLastFiveMinutes(studentId)
        }
    }

    fun updateMap(locationData: StudentData) {
        val latLng = LatLng(locationData.latitude, locationData.longitude)
        googleMap.addMarker(MarkerOptions().position(latLng).title(locationData.studentId))

        if (!hasInitialCameraMoved) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
            hasInitialCameraMoved = true
        }

        drawPolyline(locationData.studentId)
    }

    fun drawLastFiveMinutes(studentId: String) {
        val locationDataList = getLastFiveMinutesData(studentId)
        val latLngPoints = locationDataList.map { LatLng(it.latitude, it.longitude) }

        val color = getColorForStudent(studentId)

        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(color)
            .width(5f)
            .geodesic(true)

        googleMap.addPolyline(polylineOptions)

        latLngPoints.forEach { latLng ->
            googleMap.addMarker(MarkerOptions().position(latLng).title(studentId))
        }

        if (latLngPoints.isNotEmpty() && !hasInitialCameraMoved) {
            val boundsBuilder = LatLngBounds.builder()
            latLngPoints.forEach { boundsBuilder.include(it) }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 10))
            hasInitialCameraMoved = true
        }
    }

    private fun drawPolyline(studentId: String) {
        val locationDataList = getLastFiveMinutesData(studentId)
        val latLngPoints = locationDataList.map { LatLng(it.latitude, it.longitude) }

        val color = getColorForStudent(studentId)

        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(color)
            .width(5f)
            .geodesic(true)

        googleMap.addPolyline(polylineOptions)

        if (latLngPoints.isNotEmpty() && !hasInitialCameraMoved) {
            val boundsBuilder = LatLngBounds.builder()
            latLngPoints.forEach { boundsBuilder.include(it) }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 10))
            hasInitialCameraMoved = true
        }
    }

    private fun getLastFiveMinutesData(studentId: String): List<StudentData> {
        val currentTime = System.currentTimeMillis()
        val fiveMinutesAgo = currentTime - 5 * 60 * 1000
        return databaseHelper.getLocationDataByStudentId(studentId).filter { it.timestamp >= fiveMinutesAgo }
    }

    private fun getColorForStudent(studentId: String): Int {
        return studentColors.getOrPut(studentId) {
            val random = java.util.Random()
            Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }
    }
}