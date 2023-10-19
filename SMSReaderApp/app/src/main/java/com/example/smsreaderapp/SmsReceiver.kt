package com.example.smsreaderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("SmsReceiver", "Received SMS intent")
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.let {
            if (it.isNotEmpty()) {
                val messageBody = it[0].messageBody
                // For simplicity, let's show a Toast with the SMS content
                Log.d("SmsReceiver", messageBody)  // Log the SMS details
                Log.d("SmsReceiver", it[0].displayOriginatingAddress)
                Toast.makeText(context, messageBody, Toast.LENGTH_LONG).show()
            }
        }

    }
}
