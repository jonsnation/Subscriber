package com.example.subscriber.utils

import android.util.Log
import com.example.subscriber.data.DatabaseHelper
import com.example.subscriber.data.StudentData
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class MqttHandler(
    private val mqttClient: Mqtt3AsyncClient,
    private val databaseHelper: DatabaseHelper,
    private val updateUI: (StudentData) -> Unit
) {

    fun connectAndSubscribe() {
        mqttClient.connect().whenComplete { _, throwable ->
            if (throwable == null) {
                subscribeToTopic()
            } else {
                Log.e("MqttHandler", "MQTT connection failed", throwable)
            }
        }
    }

    private fun subscribeToTopic() {
        mqttClient.subscribeWith()
            .topicFilter("assignment/location")
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { publish ->
                val payload = String(publish.payloadAsBytes)
                Log.d("MqttHandler", "Received message: $payload")
                handleIncomingMessage(payload)
            }
            .send()
    }

    private fun handleIncomingMessage(payload: String) {
        try {
            if (payload.trim().isEmpty()) {
                Log.e("MqttHandler", "Received empty message")
                return
            }

            if (payload.trim().startsWith("{")) {
                val jsonObject = JSONObject(payload)
                val studentId = jsonObject.optString("studentId", jsonObject.optString("studentID", ""))
                if (studentId.isEmpty()) {
                    Log.e("MqttHandler", "JSON parsing error: No studentID found")
                    return
                }
                val latitude = jsonObject.getDouble("latitude")
                val longitude = jsonObject.getDouble("longitude")
                val speed = jsonObject.optDouble("speed", 0.0)

                val timestamp: Long = try {
                    jsonObject.getLong("timestamp")
                } catch (e: JSONException) {
                    try {
                        val timestampString = jsonObject.getString("timestamp")
                        parseTimestamp(timestampString)
                    } catch (e: JSONException) {
                        Log.e("MqttHandler", "No valid timestamp found, using current time")
                        System.currentTimeMillis()
                    }
                }

                val locationData = StudentData(
                    studentId = studentId,
                    latitude = latitude,
                    longitude = longitude,
                    speed = speed,
                    timestamp = timestamp
                )

                databaseHelper.createLocationData(locationData)
                updateUI(locationData)
            } else {
                Log.e("MqttHandler", "Received non-JSON message: $payload")
            }
        } catch (e: JSONException) {
            Log.e("MqttHandler", "JSON parsing error", e)
        } catch (e: Exception) {
            Log.e("MqttHandler", "Error parsing timestamp", e)
        }
    }

    private fun parseTimestamp(timestampString: String): Long {
        val dateFormats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (format in dateFormats) {
            try {
                val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                val date = dateFormat.parse(timestampString)
                if (date != null) {
                    return date.time
                }
            } catch (e: ParseException) {
                Log.e("MqttHandler", "Error parsing timestamp: $timestampString", e)
            }
        }
        throw ParseException("Unparseable date: \"$timestampString\"", 0)
    }
}