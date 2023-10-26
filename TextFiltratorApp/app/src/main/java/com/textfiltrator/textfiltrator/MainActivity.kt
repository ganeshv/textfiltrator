package com.textfiltrator.textfiltrator

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.widget.Button
import android.widget.TextView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    companion object {
        const val SMS_PERMISSION_CODE = 123
    }

    // Declare Views
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var configureButton: Button
    // List to store activity logs
    private lateinit var txtLogs: TextView
    private var eventCount = 0
    private var pollingJob: kotlinx.coroutines.Job? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted!", Toast.LENGTH_SHORT).show()
                LogManager.log("SMS permission granted")
            } else {
                Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_SHORT).show()
                LogManager.log("SMS permission denied")
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogManager.initialize(this)
        setContentView(R.layout.activity_main)

        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), SMS_PERMISSION_CODE)
        }

        // Bind Views
        txtLogs = findViewById(R.id.txtLogs)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        configureButton = findViewById(R.id.configureButton)
        statusTextView = findViewById(R.id.statusTextView)

        // Set up listeners

        startButton.setOnClickListener {
            updateFwdState(true)
        }

        stopButton.setOnClickListener {
            updateFwdState(false)
        }
        configureButton.setOnClickListener {
            // Start the configuration activity
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        val forwardingState = EncryptedPreferencesUtil.getBoolean(this, "FORWARDING_STATE", false)
        updateFwdState(forwardingState)
        pollingJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) { // check if the coroutine is still active
                updateLogList()
                delay(1000)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun updateFwdState(state: Boolean) {
        val smtpConfigStatus = EncryptedPreferencesUtil.getBoolean(this, "CONFIRMED_RECEIPT", false)
        val currentFwdState = EncryptedPreferencesUtil.getBoolean(this, "FORWARDING_STATE", false)

        if (state && smtpConfigStatus) {
            startButton.isEnabled = false
            stopButton.isEnabled = true
            statusTextView.text = "Forwarding SMS: In progress"
            if (!currentFwdState) {
                LogManager.log("Started forwarding")
                EncryptedPreferencesUtil.putBoolean(this, "FORWARDING_STATE", true)
            }
        } else {
            startButton.isEnabled = smtpConfigStatus
            stopButton.isEnabled = false
            configureButton.isEnabled = true
            statusTextView.text = "Forwarding SMS: Stopped"
            if (currentFwdState) {
                LogManager.log("Stopped forwarding")
                EncryptedPreferencesUtil.putBoolean(this, "FORWARDING_STATE", false)
            }
        }
    }

    private suspend fun updateLogList() {
        val dbHelper = LogManager.dbHelper
        val count = dbHelper.getNumberOfEntries()
        if (count != eventCount) {
            // New log entries found, or log purged
            eventCount = count
            val logs = dbHelper.getEntries(10) // Fetch only the new entries
            val logText = logs.joinToString("\n") { (timestamp, text) ->
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
                "$date: $text"
            }
            txtLogs.text = logText
        }
    }
}

object EncryptedPreferencesUtil {

    private const val PREFS_FILE_NAME = "secure_prefs"

    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        return EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getEncryptedSharedPreferences(context).getString(key, defaultValue)
    }

    fun putString(context: Context, key: String, value: String) {
        getEncryptedSharedPreferences(context).edit().putString(key, value).apply()
    }

    // You can add similar functions for getInt, getBoolean, etc. if needed.
    fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return getEncryptedSharedPreferences(context).getInt(key, defaultValue)
    }

    fun putInt(context: Context, key: String, value: Int) {
        getEncryptedSharedPreferences(context).edit().putInt(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getEncryptedSharedPreferences(context).getBoolean(key, defaultValue)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getEncryptedSharedPreferences(context).edit().putBoolean(key, value).apply()
    }
}

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "LogDatabase.db"
        private const val TABLE_NAME = "log_table"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_TEXT = "text_string"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableStatement = "CREATE TABLE $TABLE_NAME ($COLUMN_TIMESTAMP INTEGER PRIMARY KEY, $COLUMN_TEXT TEXT)"
        db?.execSQL(createTableStatement)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    suspend fun addEntry(timestamp: Long, text: String) = withContext(Dispatchers.IO) {
        val values = ContentValues()
        values.put(COLUMN_TIMESTAMP, timestamp)
        values.put(COLUMN_TEXT, text)
        writableDatabase.insert(TABLE_NAME, null, values)
    }

    suspend fun getEntries(n: Int): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<Pair<Long, String>>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_TIMESTAMP DESC LIMIT ?", arrayOf(n.toString()))
        val timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP)
        val textIndex = cursor.getColumnIndex(COLUMN_TEXT)

        if (timestampIndex != -1 && textIndex != -1 && cursor.moveToFirst()) {
            do {
                val timestamp = cursor.getLong(timestampIndex)
                val text = cursor.getString(textIndex)
                logs.add(Pair(timestamp, text))
            } while (cursor.moveToNext())
        }
        cursor.close()
        logs
    }

    suspend fun getNumberOfEntries(): Int = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        count
    }

    suspend fun truncateLog(entries: Int) = withContext(Dispatchers.IO) {
        val db = writableDatabase

        // This method removes the oldest records keeping only `entries` latest records.
        db.execSQL("DELETE FROM $TABLE_NAME WHERE $COLUMN_TIMESTAMP NOT IN (SELECT $COLUMN_TIMESTAMP FROM $TABLE_NAME ORDER BY $COLUMN_TIMESTAMP DESC LIMIT $entries)")
    }
}

object LogManager {
    lateinit var dbHelper: DatabaseHelper

    fun initialize(context: Context) {
        dbHelper = DatabaseHelper(context.applicationContext)
    }

    fun log(text: String, context: Context? = null) {
        if (!::dbHelper.isInitialized) {
            if (context == null) {
                throw Exception("LogManager not initialized!")
            } else {
                initialize(context)
            }
        }
        val currentTimestamp = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            dbHelper.addEntry(currentTimestamp, text)
            val count = dbHelper.getNumberOfEntries()
            if (count > 1000) {
                dbHelper.truncateLog(750)
            }
        }
    }
}
