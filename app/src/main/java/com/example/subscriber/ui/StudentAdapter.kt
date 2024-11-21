package com.example.subscriber.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.subscriber.data.DatabaseHelper
import com.example.subscriber.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentAdapter(
    private val studentIds: MutableList<String>,
    private val databaseHelper: DatabaseHelper
) : RecyclerView.Adapter<StudentAdapter.PublisherViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublisherViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_publisher, parent, false)
        return PublisherViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PublisherViewHolder, position: Int) {
        val studentId = studentIds[position]
        holder.studentIdTextView.text = studentId

        // Fetch min, max, and avg speed for the student
        val locationDataList = databaseHelper.getLocationDataByStudentId(studentId)
        if (locationDataList.isNotEmpty()) {
            val speeds = locationDataList.map { it.speed }
            val minSpeed = speeds.minOrNull() ?: 0.0
            val maxSpeed = speeds.maxOrNull() ?: 0.0
            val avgSpeed = speeds.average()
            val latestTimestamp = locationDataList.maxOf { it.timestamp }

            holder.minSpeedTextView.text = "Min Speed: %.2f km/h".format(minSpeed)
            holder.maxSpeedTextView.text = "Max Speed: %.2f km/h".format(maxSpeed)

            val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
            holder.latestTimestampTextView.text = "Latest: ${dateFormat.format(Date(latestTimestamp))}"

            holder.viewMoreButton.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, SummaryActivity::class.java).apply {
                    putExtra("studentId", studentId)
                    putExtra("startDate", locationDataList.minOf { it.timestamp })
                    putExtra("endDate", locationDataList.maxOf { it.timestamp })
                    putExtra("minSpeed", minSpeed)
                    putExtra("maxSpeed", maxSpeed)
                    putExtra("avgSpeed", avgSpeed)
                }
                context.startActivity(intent)
            }
        } else {
            holder.minSpeedTextView.text = "Min Speed: N/A"
            holder.maxSpeedTextView.text = "Max Speed: N/A"
            holder.latestTimestampTextView.text = "Latest: N/A"
        }
    }

    override fun getItemCount(): Int = studentIds.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newStudentIds: List<String>) {
        studentIds.clear()
        studentIds.addAll(newStudentIds)
        notifyDataSetChanged()
    }

    class PublisherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val studentIdTextView: TextView = view.findViewById(R.id.studentIdTextView)
        val minSpeedTextView: TextView = view.findViewById(R.id.minSpeedTextView)
        val maxSpeedTextView: TextView = view.findViewById(R.id.maxSpeedTextView)
        val latestTimestampTextView: TextView = view.findViewById(R.id.latestTimestampTextView)
        val viewMoreButton: Button = view.findViewById(R.id.viewMoreButton)
    }
}