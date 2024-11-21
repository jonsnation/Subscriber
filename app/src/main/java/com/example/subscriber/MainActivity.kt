package com.example.subscriber

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.subscriber.utils.MapHandler
import com.example.subscriber.utils.MqttHandler
import com.example.subscriber.data.DatabaseHelper
import com.example.subscriber.data.StudentData
import com.example.subscriber.ui.StudentAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mqttClient: Mqtt3AsyncClient
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var googleMap: GoogleMap
    private lateinit var publisherAdapter: StudentAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mqttHandler: MqttHandler
    private lateinit var mapHandler: MapHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this, null)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mqttClient = MqttClient.builder()
            .useMqttVersion3()
            .serverHost("broker-816035483.sundaebytestt.com")
            .serverPort(1883)
            .buildAsync()

        mqttHandler = MqttHandler(mqttClient, databaseHelper, ::updateUI)
        mqttHandler.connectAndSubscribe()

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val studentIds = databaseHelper.getAllUniqueStudentIds().toMutableList()
        publisherAdapter = StudentAdapter(studentIds, databaseHelper)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = publisherAdapter
    }

    private fun updateUI(locationData: StudentData) {
        runOnUiThread {
            val studentIds = databaseHelper.getAllUniqueStudentIds()
            publisherAdapter.updateData(studentIds)
            mapHandler.updateMap(locationData)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        mapHandler = MapHandler(googleMap, databaseHelper)
        mapHandler.initializeMap()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 10f))
            }
        }

        // Draw the last 5 minutes of movement for all students
        val studentIds = databaseHelper.getAllUniqueStudentIds()
        studentIds.forEach { studentId ->
            mapHandler.drawLastFiveMinutes(studentId)
        }
    }
}