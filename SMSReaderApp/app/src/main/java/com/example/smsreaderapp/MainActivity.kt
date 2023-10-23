/*
package com.example.smsreaderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smsreaderapp.ui.theme.SMSReaderAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SMSReaderAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SMSReaderAppTheme {
        Greeting("Android")
    }
}
*/
package com.example.smsreaderapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.temporal.ValueRange

class MainActivity : Activity() {

    companion object {
        const val SMS_PERMISSION_CODE = 123
    }

    // Declare Views
    private lateinit var activityLogRecyclerView: RecyclerView
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var configureButton: Button
    // List to store activity logs
    private val activityLogs = mutableListOf<String>()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted!", Toast.LENGTH_SHORT).show()
                logActivity("SMS permission granted")
            } else {
                Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_SHORT).show()
                logActivity("SMS permission denied")
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), SMS_PERMISSION_CODE)
        }

        // Bind Views
        activityLogRecyclerView = findViewById(R.id.recyclerView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        configureButton = findViewById(R.id.configureButton)
        statusTextView = findViewById(R.id.statusTextView)

        // Set up RecyclerView
        activityLogRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = LogAdapter(activityLogs)
        activityLogRecyclerView.adapter = adapter

        // Set up listeners

        startButton.setOnClickListener {
            // Start forwarding logic here
            logActivity("Started forwarding")
            statusTextView.text = "Forwarding SMS: In progress"
        }

        stopButton.setOnClickListener {
            // Stop forwarding logic here
            logActivity("Stopped forwarding")
            statusTextView.text = "Forwarding SMS: Stopped"
        }
        configureButton.setOnClickListener {
            // Start the configuration activity
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        }
    }

    private fun logActivity(log: String) {
        if (activityLogs.size >= 10) {
            activityLogs.removeAt(0)  // remove the oldest log
        }
        activityLogs.add(log)
        activityLogRecyclerView.adapter?.notifyDataSetChanged()
        activityLogRecyclerView.smoothScrollToPosition(activityLogs.size - 1) // scroll to the latest log
    }

}

// Adapter for the RecyclerView
class LogAdapter(private val logs: List<String>) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {
    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = TextView(parent.context)
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = logs[position]
    }

    override fun getItemCount() = logs.size
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

}
