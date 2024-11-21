package com.example.subscriber.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, DB_NAME, factory, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createSubscriberTableQuery = ("CREATE TABLE SubscriberInfo (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "studentID TEXT," +
                "latitude REAL," +
                "longitude REAL," +
                "speed REAL," +
                "timestamp INTEGER)")

        db.execSQL(createSubscriberTableQuery)
        Log.d("DatabaseHelper", "Database created at: ${db.path}")
        logAllData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS SubscriberInfo")
        onCreate(db)
    }

    fun createLocationData(locationData: StudentData) {
        val values = ContentValues().apply {
            put("studentID", locationData.studentId)
            put("latitude", locationData.latitude)
            put("longitude", locationData.longitude)
            put("speed", locationData.speed)
            put("timestamp", locationData.timestamp)
        }

        val db = this.writableDatabase
        db.insert("SubscriberInfo", null, values)
        logAllData(db)
    }

    fun getLocationDataByStudentId(studentId: String): List<StudentData> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM SubscriberInfo WHERE studentID = ?", arrayOf(studentId))
        val locationDataList = mutableListOf<StudentData>()
        if (cursor.moveToFirst()) {
            do {
                val studentData = StudentData(
                    studentId = cursor.getString(cursor.getColumnIndexOrThrow("studentID")),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    speed = cursor.getDouble(cursor.getColumnIndexOrThrow("speed")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
                locationDataList.add(studentData)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return locationDataList
    }

    fun getLocationDataByStudentIdAndDateRange(studentId: String, startDate: Long, endDate: Long): List<StudentData> {
        val locationDataList = mutableListOf<StudentData>()
        val db = this.readableDatabase
        val selection = "studentID = ? AND timestamp BETWEEN ? AND ?"
        val selectionArgs = arrayOf(studentId, startDate.toString(), endDate.toString())
        val cursor = db.query(
            "SubscriberInfo",
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val studentData = StudentData(
                    studentId = cursor.getString(cursor.getColumnIndexOrThrow("studentID")),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    speed = cursor.getDouble(cursor.getColumnIndexOrThrow("speed")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
                locationDataList.add(studentData)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return locationDataList
    }

    fun getAllUniqueStudentIds(): List<String> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT studentID FROM SubscriberInfo", null)
        val studentIds = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                val studentId = cursor.getString(cursor.getColumnIndexOrThrow("studentID"))
                studentIds.add(studentId)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return studentIds
    }

    private fun logAllData(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT * FROM SubscriberInfo", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val studentID = cursor.getString(cursor.getColumnIndexOrThrow("studentID"))
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                val speed = cursor.getDouble(cursor.getColumnIndexOrThrow("speed"))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                Log.d("DatabaseHelper", "Record: id=$id, studentID=$studentID, Latitude=$latitude, Longitude=$longitude, Speed=$speed, Timestamp=$timestamp")
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    companion object {
        private const val DB_NAME = "subscriber.db"
        private const val DB_VERSION = 2
    }
}