package com.example.subscriber.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.subscriber.data.DatabaseHelper
import com.example.subscriber.R
import com.example.subscriber.utils.SummaryMapHandler
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SummaryActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var googleMap: GoogleMap
    private lateinit var studentId: String
    private var startDate: Long = 0L
    private var endDate: Long = System.currentTimeMillis()
    private lateinit var mapHandler: SummaryMapHandler

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val mainView = findViewById<View>(R.id.summary)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this, null)

        studentId = intent.getStringExtra("studentId") ?: return

        findViewById<TextView>(R.id.studentIdTextView).text = studentId

        val locationDataList = databaseHelper.getLocationDataByStudentId(studentId)
        if (locationDataList.isNotEmpty()) {
            startDate = locationDataList.minOf { it.timestamp }
            endDate = locationDataList.maxOf { it.timestamp }
        }

        val minSpeed = intent.getDoubleExtra("minSpeed", 0.0)
        val maxSpeed = intent.getDoubleExtra("maxSpeed", 0.0)
        val avgSpeed = intent.getDoubleExtra("avgSpeed", 0.0)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val startDateString = dateFormat.format(Date(startDate))
        val endDateString = dateFormat.format(Date(endDate))

        findViewById<TextView>(R.id.startDateTextView).text = "Start Date: $startDateString"
        findViewById<TextView>(R.id.endDateTextView).text = "End Date: $endDateString"
        findViewById<TextView>(R.id.minSpeedTextView).text = "Min Speed: %.2f km/h".format(minSpeed)
        findViewById<TextView>(R.id.maxSpeedTextView).text = "Max Speed: %.2f km/h".format(maxSpeed)
        findViewById<TextView>(R.id.avgSpeedTextView).text = "Avg Speed: %.2f km/h".format(avgSpeed)

        val startDateButton: Button = findViewById(R.id.startDateButton)
        val endDateButton: Button = findViewById(R.id.endDateButton)
        val updateMapButton: Button = findViewById(R.id.updateMapButton)

        startDateButton.setOnClickListener {
            showDatePickerDialog { year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                startDate = calendar.timeInMillis
                findViewById<TextView>(R.id.startDateTextView).text = "Start Date: ${dateFormat.format(Date(startDate))}"
            }
        }

        endDateButton.setOnClickListener {
            showDatePickerDialog { year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                endDate = calendar.timeInMillis
                findViewById<TextView>(R.id.endDateTextView).text = "End Date: ${dateFormat.format(Date(endDate))}"
            }
        }

        updateMapButton.setOnClickListener {
            mapHandler.updateMarkersAndPolyline(startDate, endDate)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun showDatePickerDialog(onDateSet: (year: Int, month: Int, day: Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            onDateSet(selectedYear, selectedMonth, selectedDay)
        }, year, month, day)

        datePickerDialog.show()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        mapHandler = SummaryMapHandler(this, googleMap, databaseHelper, studentId, startDate, endDate)
        mapHandler.drawMarkersAndPolyline()
    }

    @SuppressLint("SetTextI18n")
    fun updateSpeedTextViews(minSpeed: Double, maxSpeed: Double, avgSpeed: Double) {
        findViewById<TextView>(R.id.minSpeedTextView).text = "Min Speed: %.2f km/h".format(minSpeed)
        findViewById<TextView>(R.id.maxSpeedTextView).text = "Max Speed: %.2f km/h".format(maxSpeed)
        findViewById<TextView>(R.id.avgSpeedTextView).text = "Avg Speed: %.2f km/h".format(avgSpeed)
    }
}